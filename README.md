# Kokki

**Distributed GitHub Hooks Management for Development Teams**

Kokki centralizes git hooks in GitHub repositories and distributes them to local development environments. Teams can version-control their hooks and ensure consistent code quality across all developer workstations.

_event sourced, hooks management tool_

## Features

- **Multi-Provider Architecture**: SSH (via GitHub CLI/git) and PAT token authentication
- **Centralized Hook Management**: Store hooks in `.github/hooks/` directory  
- **Local Hook Installation**: Setup and sync hooks to local `.git/hooks/`
- **Dual Execution**: Run with `clj` command or standalone JAR
- **Custom Directories**: Configurable source and target hook directories
- **Verbose Debugging**: Detailed authentication and operation logging

## Quick Start

```bash
# Build standalone JAR
clj -T:uberjar

# Setup local hooks directory  
java -jar target/kokki.jar setup

# List hooks from GitHub repository (SSH)
java -jar target/kokki.jar -r owner/repo --ssh list

# Sync all hooks to local .git/hooks/ (SSH)  
java -jar target/kokki.jar -r owner/repo --ssh sync

# Alternative with PAT token
java -jar target/kokki.jar -r owner/repo -t ghp_xxxxx sync
```

### Real Examples from Development

```bash
# List hooks from demo repository
➜ clj -Scp target/kokki-standalone.jar -m kokki.app --src-dir "./demo-hooks/.github/hooks" list

Available hooks:
- pre-push.sh
- pre-commit.sh  
- commit-msg.sh

# Setup hooks directory
➜ clj -Scp target/kokki-standalone.jar -m kokki.app --src-dir "./demo-hooks/.github/hooks" setup
Performing :setup:done

# List hooks from remote repository with SSH
➜ clj -Scp target/kokki-standalone.jar -m kokki.app --ssh --repo "comcast-mcu/who" --src-dir "ops/hooks" list

Available hooks:
- pre-push
```

## Command Reference

### Core Commands
```bash
setup    # Create local hooks directory structure
list     # Display available hooks from repository  
sync     # Download and install all hooks locally
install  # Install specific hook by name
```

### Authentication Options
```bash
--ssh             # Use SSH keys (GitHub CLI or git clone)
-t, --token       # GitHub Personal Access Token
-r, --repo        # Repository in owner/repo format
```

### Directory Options  
```bash
-d, --hooks-dir   # Local hooks directory [default: .git/hooks]
--src-dir         # Remote source directory [default: .github/hooks]
```

### Execution Methods
```bash
# Clojure CLI
clj -M:run -r owner/repo --ssh sync

# Standalone JAR
java -jar target/kokki.jar -r owner/repo --ssh sync
```

## Repository Structure

Store your team's git hooks in a GitHub repository:

```
your-hooks-repo/
├── .github/hooks/          # Default source directory
│   ├── pre-commit.sh       # Runs before commits
│   ├── pre-push.sh         # Runs before pushes  
│   ├── commit-msg.sh       # Validates commit messages
│   └── post-merge.sh       # Runs after merges
├── ops/hooks/              # Custom source directory
│   └── pre-push            # Hook without .sh extension
└── README.md
```

### Hook Examples from Demo Repository

**pre-commit.sh** - Code quality checks:
```bash
#!/bin/bash
# Check for TODO comments, console.log, large files
if git diff --cached --name-only | grep -E "\.(js|py)$"; then
    echo "Found staged files with potential issues"
    exit 1
fi
```

**commit-msg.sh** - Conventional commit validation:
```bash
#!/bin/bash
# Validate: feat:, fix:, docs:, refactor:, test:, chore:
if ! grep -qE "^(feat|fix|docs|refactor|test|chore):" "$1"; then
    echo "Commit message must start with feat:, fix:, etc."
    exit 1
fi
```

**pre-push.sh** - Branch protection and testing:
```bash
#!/bin/bash
# Prevent direct pushes to main/master
current_branch=$(git rev-parse --abbrev-ref HEAD)
if [[ "$current_branch" == "main" || "$current_branch" == "master" ]]; then
    echo "Direct pushes to $current_branch are not allowed"
    exit 1
fi
```

## Authentication Setup

### SSH Authentication (Recommended)

Uses existing SSH keys without token management:

```bash
# 1. Generate SSH key (if needed)
ssh-keygen -t ed25519 -C "your_email@example.com"
ssh-add ~/.ssh/id_ed25519

# 2. Add public key to GitHub settings
cat ~/.ssh/id_ed25519.pub  
# Copy output to: https://github.com/settings/keys

# 3. Install GitHub CLI (optional, improves performance)
brew install gh && gh auth login

# 4. Use with Kokki
java -jar target/kokki.jar -r owner/repo --ssh sync
```

### Personal Access Token

Traditional token authentication:

```bash
# 1. Generate token at: https://github.com/settings/tokens
# 2. Grant 'repo' scope for private repositories
# 3. Use with Kokki
java -jar target/kokki.jar -r owner/repo -t ghp_xxxxxxxxxxxx sync
```

## Architecture

### Provider Pattern Implementation

Kokki uses a pluggable provider architecture defined in `src/kokki/core.clj:9-27`:

```clojure
(defn init-provider! [provider options]
  (case provider
    :github-pat (github/->TokenAuthGithub token repo dirs)
    :github-ssh (github/->SshGithubProvider repo dirs)  
    :inline     (inline/->InlineProvider dirs)))
```

**Provider Types:**
- **GitHub PAT** (`github.clj`): REST API with token authentication
- **GitHub SSH** (`github.clj`): Uses GitHub CLI or git SSH clone
- **Inline** (`inline.clj`): Local file system operations

### Action Execution Flow

From `src/kokki/core.clj:29-53`, actions are processed through a unified interface:

```clojure
(defn execute-action [Provider {:keys [action options]}]
  (case action
    :list   (proto/list-hooks Provider)
    :setup  (proto/apply-hooks Provider) 
    :install (proto/apply-hooks Provider specific-hook)))
```

### Dependencies

- **clj-http**: GitHub API client 
- **babashka/fs**: File system operations
- **babashka/process**: Shell command execution
- **tools.cli**: Command line parsing
- **cheshire**: JSON processing

## Development

### Build Commands
```bash
clj -T:uberjar              # Build standalone JAR
clj -M:test                 # Run test suite  
clj -M:check                # Lint code
clj -M:repl                 # Start development REPL
```

### Project Structure
```
kokki/
├── src/kokki/
│   ├── app.clj            # Main entry point and CLI handling
│   ├── cli.clj            # Command line argument parsing  
│   ├── core.clj           # Provider initialization and action execution
│   ├── constant.clj       # Application constants
│   └── providers/
│       ├── github.clj     # GitHub API and SSH implementations
│       ├── inline.clj     # Local file system provider
│       ├── proto.clj      # Provider protocol definition
│       └── utils.clj      # Shared utilities
├── demo-hooks/            # Example hooks repository
├── target/kokki.jar       # Standalone executable
└── deps.edn              # Dependencies and build config
```

## Implementation Details

### Commit History

**Initial Implementation** (commit `6c4610a`):
- Added comprehensive git hooks management with SSH/PAT authentication
- Implemented multi-provider architecture with GitHub and inline providers
- Created CLI interface with setup, list, sync, and install commands
- Added support for both clj command and standalone JAR execution
- Included configurable directories and verbose debugging
- Built complete documentation with usage examples

### Error Handling

Clear error messages for common scenarios:
- **Invalid repository format**: Repository must be `owner/repo` format
- **Authentication failures**: Check GitHub token permissions or SSH keys
- **Network issues**: Verify connectivity and repository accessibility  
- **File permissions**: Ensure write access to hooks directory

## License

Eclipse Public License 2.0

See [LICENSE](LICENSE) file for full license text.

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
