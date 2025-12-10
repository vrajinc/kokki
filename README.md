# Kokki 🪝

**Kokki** is a distributed GitHub hooks management tool that helps you easily setup and manage git hooks across your projects. It allows you to centralize your git hooks in a GitHub repository and distribute them to local development environments.

_event sourced, hooks management tool_

## Features

- 🚀 **Easy Setup**: Quickly setup local hooks directory
- 🔄 **Sync from GitHub**: Automatically sync hooks from your GitHub repository
- 📋 **List Available Hooks**: View all hooks available in your repository
- ⚡ **Multiple Run Methods**: Run with either `clj` command or standalone JAR
- 🔧 **Configurable**: Flexible configuration for different repositories and tokens
- 📝 **CLI Interface**: User-friendly command-line interface with comprehensive help

## Quick Start

### Prerequisites

- Java 8+ installed
- Clojure CLI tools installed (for `clj` commands)
- A GitHub repository with hooks stored in `.github/hooks/` directory
- GitHub personal access token (for private repositories)

### Installation

Clone and build the project:

```bash
git clone <your-repo-url>
cd kokki
clj -T:uberjar
```

This will create `target/kokki.jar` - a standalone executable.

## Usage

### Authentication Methods

Kokki supports two authentication methods:

1. **GitHub Personal Access Token (PAT)** - Traditional token-based authentication
2. **SSH Authentication** - Uses your SSH keys via GitHub CLI or git SSH

### Running with Clojure CLI

You can run kokki directly using the Clojure CLI:

```bash
# Show help
clj -M:run --help

# Setup hooks directory
clj -M:run setup

# Setup custom hooks directory
clj -M:run setup -d ./my-hooks

# Sync hooks using PAT token
clj -M:run -r owner/repo -t github_token sync

# Sync hooks using SSH authentication
clj -M:run -r owner/repo --ssh sync

# List available hooks using PAT token
clj -M:run -r owner/repo -t github_token list

# List available hooks using SSH authentication
clj -M:run -r owner/repo --ssh list
```

### Running with JAR

Alternatively, use the standalone JAR file:

```bash
# Show help
java -jar target/kokki.jar --help

# Setup hooks directory
java -jar target/kokki.jar setup

# Setup custom hooks directory
java -jar target/kokki.jar setup -d ./my-hooks

# Sync hooks using PAT token
java -jar target/kokki.jar -r owner/repo -t github_token sync

# Sync hooks using SSH authentication  
java -jar target/kokki.jar -r owner/repo --ssh sync

# List available hooks using PAT token
java -jar target/kokki.jar -r owner/repo -t github_token list

# List available hooks using SSH authentication
java -jar target/kokki.jar -r owner/repo --ssh list
java -jar target/kokki.jar -r owner/repo -t github_token list
```

## Command Line Options

```
Options:
  -h, --help                       Show help
  -v, --verbose                    Verbose output
  -r, --repo REPO                  GitHub repository (owner/repo)
  -t, --token TOKEN                GitHub personal access token
  -s, --ssh                        Use SSH authentication (via GitHub CLI or git)
  -d, --hooks-dir DIR              Local hooks directory [default: .git/hooks]

Actions:
  setup    Setup local hooks directory
  sync     Sync hooks from GitHub repository  
  list     List available hooks
  install  Install a specific hook
```

## Authentication

### SSH Authentication (Recommended)

SSH authentication is the most convenient method as it uses your existing SSH keys and doesn't require managing tokens.

**Prerequisites:**
- SSH keys set up with GitHub
- Either GitHub CLI (`gh`) installed and authenticated, OR
- Git configured with SSH access to GitHub

**SSH Authentication Methods (in order of precedence):**

1. **GitHub CLI** - If `gh` is installed and authenticated, kokki will use the GitHub CLI for API requests
2. **Git SSH Clone** - If GitHub CLI is not available, kokki will clone the repository using SSH and extract hooks locally

```bash
# Setup SSH authentication
ssh-keygen -t ed25519 -C "your_email@example.com"  # If you don't have SSH keys
ssh-add ~/.ssh/id_ed25519                           # Add to SSH agent
# Add public key to GitHub: https://github.com/settings/keys

# Optional: Install and authenticate GitHub CLI for better performance
gh auth login

# Use SSH authentication with kokki
clj -M:run -r owner/repo --ssh sync
clj -M:run -r owner/repo --ssh list
```

### Personal Access Token (PAT)

Traditional token-based authentication for users who prefer not to use SSH.

**Setup:**
1. Go to GitHub Settings → Developer settings → Personal access tokens
2. Generate a new token with `repo` scope (for private repos) or `public_repo` scope (for public repos)
3. Use the token with the `-t` option

```bash
# Using PAT token
clj -M:run -r owner/repo -t ghp_xxxxxxxxxxxx sync
clj -M:run -r owner/repo -t ghp_xxxxxxxxxxxx list
```

## Repository Structure

For kokki to work, your GitHub repository should have the following structure:

```
your-repo/
├── .github/
│   └── hooks/
│       ├── pre-commit.sh
│       ├── pre-push.sh
│       ├── post-merge.sh
│       └── commit-msg.sh
└── ... (your project files)
```

All hook files should be executable shell scripts (`.sh` files) stored in `.github/hooks/`.

## Examples

### Basic Workflow with SSH (Recommended)

1. **Setup SSH keys and GitHub CLI (optional but recommended):**
   ```bash
   # Setup SSH keys if you don't have them
   ssh-keygen -t ed25519 -C "your_email@example.com"
   ssh-add ~/.ssh/id_ed25519
   
   # Add public key to GitHub: https://github.com/settings/keys
   
   # Install GitHub CLI for better performance (optional)
   gh auth login
   ```

2. **Setup a hooks directory:**
   ```bash
   clj -M:run setup
   ```

3. **List available hooks from your repository using SSH:**
   ```bash
   clj -M:run -r myorg/hooks-repo --ssh list
   ```

4. **Sync all hooks from repository using SSH:**
   ```bash
   clj -M:run -r myorg/hooks-repo --ssh sync
   ```

### Basic Workflow with PAT Token

1. **Setup a hooks directory:**
   ```bash
   clj -M:run setup
   ```

2. **List available hooks from your repository:**
   ```bash
   clj -M:run -r myorg/hooks-repo -t ghp_xxxxxxxxxxxx list
   ```

3. **Sync all hooks from repository:**
   ```bash
   clj -M:run -r myorg/hooks-repo -t ghp_xxxxxxxxxxxx sync
   ```

### Using Environment Variables

You can set environment variables to avoid typing tokens repeatedly:

```bash
# For PAT tokens
export GITHUB_TOKEN=ghp_xxxxxxxxxxxx
export HOOKS_REPO=myorg/hooks-repo

# Now you can run commands without specifying token/repo
clj -M:run -r $HOOKS_REPO -t $GITHUB_TOKEN sync

# For SSH authentication, you only need the repo
export HOOKS_REPO=myorg/hooks-repo
clj -M:run -r $HOOKS_REPO --ssh sync
```

### Custom Hooks Directory

```bash
# Setup hooks in a custom directory
clj -M:run setup -d ./project-hooks

# Sync to custom directory using SSH
clj -M:run -r myorg/hooks-repo --ssh -d ./project-hooks sync

# Sync to custom directory using PAT token
clj -M:run -r myorg/hooks-repo -t $GITHUB_TOKEN -d ./project-hooks sync
```

### Verbose Output

Use the `--verbose` flag to see detailed authentication and processing information:

```bash
# See which authentication method is being used
clj -M:run -r myorg/hooks-repo --ssh --verbose list
```

## Development

### Available Commands

```bash
# Start REPL
clj -M:repl

# Run tests
clj -M:test

# Lint code
clj -M:check

# Build JAR
clj -T:uberjar
```

### Project Structure

```
kokki/
├── deps.edn              # Dependencies and build configuration
├── src/kokki/           
│   └── core.clj         # Main application code
├── resources/           # Resource files
├── target/              # Build artifacts
│   └── kokki.jar       # Standalone executable JAR
└── README.md           # This file
```

### Dependencies

- **Clojure 1.11.1** - Core language
- **clj-http** - GitHub API client
- **tools.cli** - Command line parsing
- **babashka/fs** - File system operations  
- **babashka/process** - Shell command execution
- **cheshire** - JSON processing
- **tools.logging** - Logging infrastructure

## GitHub Integration

### Authentication Methods

Kokki supports multiple authentication methods for accessing GitHub repositories:

#### 1. SSH Authentication (Recommended)

SSH authentication uses your existing SSH keys and doesn't require managing tokens.

**How it works:**
- **GitHub CLI Method**: If `gh` CLI is installed and authenticated, kokki uses it for API requests
- **Git SSH Method**: If GitHub CLI is not available, kokki clones the repository via SSH and reads hooks locally

**Setup:**
```bash
# Generate SSH key if you don't have one
ssh-keygen -t ed25519 -C "your_email@example.com"

# Add to SSH agent
ssh-add ~/.ssh/id_ed25519

# Add public key to GitHub
cat ~/.ssh/id_ed25519.pub
# Copy output and add at: https://github.com/settings/keys

# Optional: Install GitHub CLI for better performance
# macOS: brew install gh
# Other platforms: https://github.com/cli/cli#installation
gh auth login
```

#### 2. Personal Access Token (PAT)

Traditional token-based authentication.

**Setup:**
1. Go to GitHub Settings → Developer settings → Personal access tokens
2. Generate a new token with `repo` scope (for private repos) or `public_repo` scope (for public repos)
3. Use the token with the `-t` option

### API Usage

Kokki interacts with GitHub through multiple methods:

- **GitHub CLI API**: Direct API calls via `gh api` command (fastest)
- **Git SSH Clone**: Repository cloning via SSH for hook extraction
- **GitHub REST API**: Direct HTTP API calls with PAT tokens

### Repository Access

- **Public repositories**: Both SSH and PAT methods work
- **Private repositories**: Requires appropriate SSH key access or PAT token with repo permissions
- **Organization repositories**: Works with both methods if you have access

## Error Handling

Kokki provides clear error messages for common issues:

- **Invalid repository format**: Repository must be in `owner/repo` format
- **Authentication failures**: Check your GitHub token permissions
- **Network issues**: Verify internet connectivity and repository accessibility
- **File system errors**: Ensure proper permissions for hooks directory

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Run tests and linting: `clj -M:test && clj -M:check`
5. Submit a pull request

## License

[Add your license here]

## Changelog

### v0.1.0
- Initial release
- Basic GitHub hooks synchronization  
- CLI interface with setup, sync, and list commands
- Support for both `clj` and JAR execution methods
- Configurable hooks directory
- **SSH Authentication Support**: Use SSH keys via GitHub CLI or git SSH
- **PAT Token Authentication**: Traditional GitHub personal access token support
- **Multiple Authentication Methods**: Automatic fallback from GitHub CLI to git SSH to PAT tokens
- GitHub API integration with comprehensive error handling
