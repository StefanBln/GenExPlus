# Contributing to GenExPlus

Thank you for your interest in **GenExPlus** (Generic Export Plus). This project is open to bug reports, documentation improvements, and code contributions.

## Before you start

1. Search [existing issues](https://github.com/StefanBln/GenExPlus/issues) — your topic may already be open.
2. Read the [FAQ](docs/FAQ.md) and [How-To guides](docs/HOWTO.md) for operator questions.
3. For security issues, see [SECURITY.md](SECURITY.md) — do **not** open a public issue for vulnerabilities.

Please read our [Code of Conduct](CODE_OF_CONDUCT.md). We expect everyone participating in issues, discussions, and pull requests to follow it.

## Development setup

```bash
git clone https://github.com/StefanBln/GenExPlus.git
cd GenExPlus
mvn package
./start.sh report.conf.example
```

Requirements: **Java 21+**, **Maven 3.9+**.

## Running tests

```bash
mvn test                         # unit tests (default)
mvn -Pintegration-tests test     # renderer + email integration
mvn -Pe2e test                   # CLI end-to-end scenarios
./scripts/e2e-run.sh             # same scenarios, artifacts in target/e2e-reports/
```

Optional local databases for DB tests:

```bash
./scripts/test-db-up.sh
mvn -Pe2e test -Dgroups=e2e-db
./scripts/test-db-down.sh
```

## Pull requests

1. Fork the repository and create a branch from `main`.
2. Keep changes focused — one logical change per PR when possible.
3. Ensure `mvn test` passes (and integration/E2E tests if you touched those areas).
4. Match existing code style and conventions in surrounding files.
5. Update `docs/` when you change configuration keys or CLI behaviour.
6. New Java and shell files must include the Apache 2.0 copyright header (see any existing `.java` file).

Maintainer review is required before merge to `main`.

## Signed commits (Verified on GitHub)

GitHub shows a **Verified** badge when commits are cryptographically signed with a key registered on your account.

### Option A — SSH signing (recommended if you already use SSH)

```bash
# Use your existing GitHub SSH key (or create one)
git config --global gpg.format ssh
git config --global user.signingkey ~/.ssh/id_ed25519.pub
git config --global commit.gpgsign true
```

Then add the **same public key** on GitHub as a **Signing key** (not only Authentication):

[github.com/settings/keys](https://github.com/settings/keys) → **New SSH key** → Key type: **Signing Key** → paste `~/.ssh/id_ed25519.pub`

Verify locally:

```bash
git commit -S -m "test signed commit"
git log --show-signature -1
```

After push, the commit should show **Verified** on GitHub.

### Option B — GPG signing

```bash
gpg --full-generate-key
gpg --list-secret-keys --keyid-format=long
git config --global user.signingkey YOUR_KEY_ID
git config --global commit.gpgsign true
gpg --armor --export YOUR_KEY_ID   # add to GitHub → Settings → SSH and GPG keys → GPG
```

### Tags and releases

Release tags trigger the GitHub Actions release workflow. Supported patterns:

- `v1.0.2` or `1.0.2`

Example:

```bash
git tag -s v1.0.2 -m "Release 1.0.2"
git push github v1.0.2
```

Use `-s` on tags as well if you want verified release tags.

## Copyright

Contributions are licensed under the [Apache License 2.0](LICENSE). By submitting a PR, you agree that your contribution may be included under that license.

## Questions

Open a [GitHub Issue](https://github.com/StefanBln/GenExPlus/issues) for questions that are not security-sensitive.
