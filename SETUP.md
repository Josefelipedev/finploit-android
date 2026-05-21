# FinPloit Android — Setup

## Pré-requisitos

- Android Studio Ladybug (2024.2) ou superior
- JDK 17+
- Emulador ou dispositivo com Android 8.0+ (API 26)

## Configuração

### 1. Google Sign-In

No [Google Cloud Console](https://console.cloud.google.com):
1. Selecione o projeto que já tem OAuth configurado no backend
2. Vá em **APIs & Services → Credentials**
3. Crie um **OAuth 2.0 Client ID** do tipo **Android**
   - Package name: `com.finploit.android`
   - SHA-1: obtenha com `./gradlew signingReport`
4. Copie o **Web Client ID** (não o Android) para a variável abaixo

### 2. Variável de ambiente

Defina antes de rodar o build:

```bash
export GOOGLE_CLIENT_ID="seu-web-client-id.apps.googleusercontent.com"
```

Ou edite diretamente em `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "GOOGLE_CLIENT_ID", "\"SEU_CLIENT_ID_AQUI\"")
```

### 3. URL da API

| Flavor | URL padrão |
|--------|-----------|
| `dev` | `http://10.0.2.2:5009/` (emulador → localhost) |
| `prod` | `https://api.finploit.com/` |

Para rodar em dispositivo físico no dev, altere para o IP da sua máquina:
```kotlin
buildConfigField("String", "API_BASE_URL", "\"http://192.168.1.X:5009/\"")
```

## Rodando

```bash
# Emulador (flavor dev)
./gradlew installDevDebug

# Ou via Android Studio: Run → devDebug
```

## Estrutura de telas (Fase 1)

```
Login
  └── Email + Senha
  └── Google Sign-In

Main
  ├── Dashboard    — saldo, receitas, despesas, últimas transações
  ├── Transações  — lista com filtro por tipo + botão adicionar
  └── Perfil      — dados do usuário + logout
```
