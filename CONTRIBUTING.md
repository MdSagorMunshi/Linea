# Contributing to LINEA

First off, thank you for considering contributing to LINEA! As a Free and Open Source Software (FOSS) project, LINEA thrives on community involvement, peer review, and transparent engineering.

Before contributing, please read our [Code of Conduct](CODE_OF_CONDUCT.md) and review our foundational tenets below.

---

## 1. Foundational Tenets & Architectural Non-Negotiables

Every feature and contribution submitted to LINEA must strictly adhere to our core principles:

1. **100% On-Device / Zero Cloud Dependency**:
   - LINEA does not communicate with any cloud server, remote API, or proprietary analytic backend.
   - Algorithms for caller identification, availability insights, T9 search, DTMF tones, and call screening must run deterministically in local memory or SQLite queries.
   - No external analytics, telemetry tracking, or crash-reporting SDKs are permitted.

2. **Native Telecom Integration**:
   - Never bypass native Android telephony contracts.
   - Calling features must integrate through Android's `TelecomManager`, `InCallService`, `ConnectionService`, and `PhoneAccount` APIs.

3. **Tactile Neumorphic (Soft UI) Aesthetics**:
   - UI contributions must follow the established design language: deep graphite background (`#181B20` to `#1E2228`), extruded physical surfaces with dual directional shadows (opposing top-left light highlight and bottom-right shadow), debossed sunken wells for inputs and active selections, tabular figures (`FontFeature("tnum")`), and designated status accents.
   - Avoid generic Material Design themes, flat borders, or arbitrary colors.

4. **User Privacy & Security**:
   - Contact numbers and call records must remain encrypted and isolated.
   - Never log personal contacts, caller phone numbers, or call recordings in logcat or debug output.

---

## 2. Getting Started

### 2.1. Fork & Clone
1. Fork the repository on GitHub: [https://github.com/MdSagorMunshi/Linea](https://github.com/MdSagorMunshi/Linea)
2. Clone your fork locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/Linea.git
   cd Linea
   ```
3. Add the upstream remote:
   ```bash
   git remote add upstream https://github.com/MdSagorMunshi/Linea.git
   ```

### 2.2. Development Environment
Refer to [BUILD.md](BUILD.md) for full SDK, JDK, and toolchain installation instructions.

---

## 3. Development Workflow

### 3.1. Branching Strategy
Create a dedicated feature branch from `master` using standard naming conventions:

- `feat/<feature-name>`: New capabilities or functional enhancements
- `fix/<bug-summary>`: Bug fixes and edge case patches
- `docs/<doc-name>`: Documentation, guides, and markdown updates
- `refactor/<module>`: Code restructuring with zero behavior changes
- `perf/<target>`: Performance and memory optimizations

```bash
git checkout master
git pull upstream master
git checkout -b feat/custom-ringtone-selector
```

### 3.2. Coding Conventions & Best Practices

- **Kotlin Style**: Adhere to official Kotlin coding conventions. Format your code with `./gradlew ktlintCheck` or Android Studio default formatting.
- **Jetpack Compose**:
  - Prefer stateless composables that accept parameters and hoist events.
  - Use `remember` and `derivedStateOf` judiciously to prevent unnecessary recompositions.
  - Use `LineaColors`, `LineaTypography`, and `LineaDimensions` for all styling.
- **State Management**:
  - Expose state via `StateFlow` from ViewModels.
  - Collect state in UI using `collectAsStateWithLifecycle()`.
- **Database & DAOs**:
  - All Room queries that modify tables must run on `Dispatchers.IO`.
  - Maintain schema migrations and annotate relational entities with foreign key cascades where appropriate.

### 3.3. Commit Message Standards
We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

```
<type>(<scope>): <short imperative summary>

[optional body explaining rationale]

[optional issue reference]
```

**Types**:
- `feat`: A new user-facing feature
- `fix`: A bug fix
- `docs`: Documentation changes
- `style`: Formatting, missing semicolons, etc. (no code change)
- `refactor`: Code refactoring without functionality changes
- `perf`: Code changes that improve performance
- `test`: Adding or updating unit tests
- `chore`: Build scripts, dependencies, or configuration changes

**Example**:
```
feat(telecom): add multi-call merge and conference switching

- Implement swapCalls and mergeConference in CallManager
- Add in-call action chip to conference active lines
- Fixes #42
```

---

## 4. Testing & Quality Standards

Before submitting a Pull Request, ensure:
1. **Compilation succeeds** without warnings:
   ```bash
   ./gradlew compileDebugKotlin
   ```
2. **All unit tests pass**:
   ```bash
   ./gradlew testDebugUnitTest
   ```
3. **Debug APK builds cleanly**:
   ```bash
   ./gradlew assembleDebug
   ```

If you add a new feature or fix a bug, please write corresponding unit tests under `app/src/test/java/`.

---

## 5. Submitting a Pull Request (PR)

1. Push your branch to your GitHub fork:
   ```bash
   git push origin feat/custom-ringtone-selector
   ```
2. Open a Pull Request against the `master` branch of `MdSagorMunshi/Linea`.
3. Provide a clear, descriptive title and fill out the PR description with:
   - **Problem / Motivation**: Why is this change needed?
   - **Solution**: How does this PR solve the issue?
   - **Screenshots / GIFs**: For UI changes, attach screenshots. Ensure NO personal contacts or numbers are visible.
   - **Testing Performed**: Detail the tests you executed.

---

## 6. Reporting Issues & Vulnerabilities

- **Bug Reports**: Open an issue on GitHub with device model, Android OS version, steps to reproduce, and expected vs. actual behavior.
- **Security Vulnerabilities**: Do NOT file public GitHub issues for security vulnerabilities. Please report them directly to `ryn@disr.it` in accordance with our [SECURITY.md](SECURITY.md).
