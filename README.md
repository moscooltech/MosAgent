# Moscool AI Agent

An AI-powered Android automation agent that understands natural-language instructions and performs tasks on your Android device.

## Features

- **Natural Language Commands** — Give plain-English instructions and the agent executes them
- **AI Provider Flexibility** — Works with OpenAI, Gemini, OpenRouter, Groq, or any OpenAI-compatible endpoint
- **Accessibility-Based Automation** — Uses Android's Accessibility Service to read and interact with UI elements
- **Social Media Automation** — Create and prepare Facebook posts (Telegram, Instagram, X coming soon)
- **Smart Safety Modes** — Safe, Assisted, and Autonomous modes with confirmation before sensitive actions
- **Agent Loop** — Observe → Reason → Act → Verify cycle with structured actions
- **Tool Registry** — 17+ registered tools for UI interaction, app launching, text generation, and more
- **Conversational Memory** — Maintains context within tasks
- **Task History** — Records and displays previous tasks
- **Secure Storage** — API keys encrypted with Android Keystore (AES-256-GCM)
- **Modern UI** — Material 3 design with dark theme support

## Architecture

```
app/src/main/java/com/moscool/agent/
├── ai/                 # AI provider abstraction
│   ├── AIProvider      # Interface for all AI providers
│   ├── OpenAIProvider  # OpenAI-compatible implementation
│   ├── PromptManager   # System and task prompts
│   └── ToolRegistry    # Formal tool definitions
├── agent/              # Core agent engine
│   ├── AgentEngine     # Main observe→reason→act→verify loop
│   ├── AgentPlanner    # AI-based task planning
│   ├── AgentMemory     # Conversational context
│   ├── TaskManager     # Task state machine
│   ├── ActionExecutor  # Execute actions on device
│   └── ActionVerifier  # Verify action results
├── automation/         # Android accessibility & UI automation
│   ├── AccessibilityController  # Accessibility Service
│   └── AppLauncher    # Launch apps by package name
├── social/             # Social media platform adapters
│   ├── SocialPlatform # Abstract interface
│   ├── FacebookAdapter # Facebook automation
│   └── TelegramAdapter # Telegram stub
├── data/               # Persistence layer
│   ├── db/             # Room database (tasks, logs)
│   ├── prefs/          # DataStore preferences
│   ├── secure/         # Android Keystore credentials
│   └── repository/     # Repository pattern
├── model/              # Data models
└── ui/                 # Jetpack Compose screens
    ├── home/           # Home screen with command input
    ├── agent/          # Live action timeline
    ├── preview/        # Social post preview & edit
    ├── history/        # Task history
    ├── settings/       # Configuration
    ├── onboarding/     # First-run setup
    ├── navigation/     # Navigation graph
    └── theme/          # Material 3 theme
```

## Requirements

- Android 8.0 (API 26) or higher
- Android Studio Hedgehog or later
- JDK 17
- An AI provider API key (OpenAI, Gemini, etc.)

## Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/your-username/moscool-agent.git
   cd moscool-agent
   ```

2. Open in Android Studio

3. Build and run on a device or emulator

4. On first launch, configure your AI provider in Settings

5. Enable the Accessibility Service when prompted

## AI Provider Configuration

The app supports any OpenAI-compatible API. Configure in Settings:

| Provider | Base URL |
|----------|----------|
| OpenAI | `https://api.openai.com/v1` |
| Google Gemini | `https://generativelanguage.googleapis.com/v1beta` |
| OpenRouter | `https://openrouter.ai/api/v1` |
| Groq | `https://api.groq.com/openai/v1` |
| Custom | Any OpenAI-compatible endpoint |

**Never commit API keys to source control.**

## Automation Modes

- **Safe Mode** (default): Asks before all sensitive actions
- **Assisted Mode**: Routine actions automatic, pauses for sensitive ones
- **Autonomous Mode**: Configurable permissions with clear warning

## Building

```bash
./gradlew clean
./gradlew lint
./gradlew test
./gradlew assembleDebug
```

## GitHub Actions

- **CI**: Runs lint, unit tests, and builds debug APK on push/PR
- **Release**: Triggered by version tags (e.g., `v1.0.0`), builds and publishes APK to GitHub Releases

### Release Secrets

Configure in GitHub repo settings:

| Secret | Description |
|--------|-------------|
| `KEYSTORE_BASE64` | Base64-encoded release keystore |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

If signing secrets are not configured, unsigned debug APKs are still built.

## Security

- API keys stored in Android Keystore (AES-256-GCM)
- No credentials in source code or logs
- Confirmation required before sensitive actions
- Emergency STOP button always available
- Never bypasses CAPTCHAs, passwords, 2FA, or biometrics

## Roadmap

### v1.0 (Current)
- AI chat and provider configuration
- Accessibility Service integration
- Facebook post generation and preparation
- Confirmation before publishing
- Task history
- GitHub APK builds

### v2.0
- Vision-based screen understanding
- Image attachment support
- Telegram, Instagram, X adapters

### v3.0
- Voice commands
- Scheduled tasks
- Advanced memory
- User-defined automation recipes

### v4.0
- Remote control via web/Telegram interface
- Multi-device support
- Advanced agent planning

## License

MIT License — see [LICENSE](LICENSE)

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md)
