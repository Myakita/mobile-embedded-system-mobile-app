# Android CI

## Требования

Для локального запуска нужны:

- JDK 25;
- Android SDK Platform 37;
- Android SDK Build Tools 37.0.0.

Gradle 9.6 загружается зафиксированным Gradle Wrapper. Отдельно устанавливать
Gradle не требуется.

## Клонирование

Клонируйте репозиторий вместе с submodules:

```bash
git clone --recurse-submodules https://github.com/Myakita/mobile-embedded-system-mobile-app.git
cd mobile-embedded-system-mobile-app
```

Если репозиторий уже клонирован, инициализируйте submodules отдельно:

```bash
git submodule update --init --recursive
```

## Локальная проверка

Запустите тот же набор задач, который выполняется в CI:

```bash
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug
```

После выполнения результаты находятся здесь:

- debug APK: `app/build/outputs/apk/debug/app-debug.apk`;
- JUnit XML: `app/build/test-results/testDebugUnitTest/`;
- HTML-отчёт тестов: `app/build/reports/tests/testDebugUnitTest/index.html`;
- отчёт Android Lint: `app/build/reports/lint-results-debug.html`.

## Диагностика ошибок

Ошибки инфраструктуры CI, установки JDK или Android SDK, Gradle Wrapper,
кэширования и публикации artifacts исправляет DevOps. Ошибки компиляции,
JVM-тестов и Android Lint передаются разработчику с полным логом без изменения
прикладного кода в DevOps-изменении.
