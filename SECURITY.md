# Security Policy

## Reporting Vulnerabilities

If you discover a security vulnerability, please report it responsibly:

1. **Do NOT** open a public GitHub issue
2. Email security details to the maintainers
3. Include steps to reproduce
4. Allow reasonable time for a fix

## Security Measures

### Data Storage
- API keys stored in Android Keystore (AES-256-GCM)
- No plaintext credentials in SharedPreferences
- Room database for local data only

### API Communication
- HTTPS only for all API calls
- No hardcoded API keys
- Credentials never logged

### Automation Safety
- Three safety modes (Safe, Assisted, Autonomous)
- Confirmation before sensitive actions
- Emergency STOP button
- No bypass of security mechanisms (CAPTCHA, 2FA, biometrics)

### Code Safety
- No arbitrary shell command execution
- No root access required
- Structured action validation before execution
- Element matching prioritizes accessibility over coordinates

## Supported Versions

| Version | Supported |
|---------|-----------|
| 1.x     | ✅        |

## Best Practices

- Keep the app updated
- Use strong API keys
- Review permissions regularly
- Use Safe Mode for new installations
