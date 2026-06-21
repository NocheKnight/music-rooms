Music Rooms - музыкальный стриминговый сервис с интеграцией с YouTube, VK Music, Sprotify и Яндекс Музыкой, позволяющий собираться с друзьями в комнатах и слушать музыку вместе.
Ключевые идеи и возможности сервиса:
- Прослушивание музыки синхронно с друзьями в виртуальных комнатах;
- Поддержка нескольких источников: YouTube, VK Музыка, Spotify, Яндекс Музыка;
- Управление очередью треков.

Технологический стек:
- Java 25, Spring Boot 4, Spring Cloud, Spring Security, Spring Data JPA
- Микросервисы: Api Gateway, Room Service, Queue Service, Media Resolver, WebSocker Server, Webhook Handler Service
- Коммуникации: WebSocket STOMP, RabbitMQ
- Аутентификация: Keycloak (JWT)
- База данных: PostgreSQL, Flyway, H2 для тестов
- Контейнеризация: Docker, Docker Compose
- Тестирование: JUnit 5, Mockito

Другие части проекта:
- Android: https://github.com/Kotlovskiy/SharedMediaPlayer
- Frontend: https://github.com/SeveralSIZE/Music-room-client

Дорожная карта:
1. Стриминг с других сервисов
2. Управление правами участников комнаты
3. Голосование за пропуск трека
4. Desktop-клиент
5. CI/CD
6. Авто тестирование