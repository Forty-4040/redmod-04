RedMod 2.0: красная кнопка и большая красная кнопка (Fabric 1.20.1, GeckoLib 4.4.9)

ПРЕДМЕТЫ И СУЩЕСТВА
- redmod:red_button_item / redmod:red_button  : Красная кнопка (6 HP)
- redmod:red_button_big_item / redmod:red_button_big : Большая Красная кнопка (10 HP, держит факел)
- Крафт большой: красная кнопка в центре, вокруг 8 железных слитков.
- Факел: ПКМ факелом по большой кнопке. Чтобы он светился, у игрока должен стоять мод на динамический
  свет (например, LambDynamicLights для Fabric 1.20.1). Без него факел виден, но не светит.
- Shift+ПКМ: взять кнопку в руки (шлем и факел при этом падают).
- В инвентаре иконка плоская, в руках 3D-модель маленькой кнопки (у обеих).
- Для проверки: /give @s redmod:red_button_big_item   или   /summon redmod:red_button_big

СБОРКА ЧЕРЕЗ GITHUB
Загрузи все файлы в репозиторий (вкладка Actions соберёт мод),
готовый файл: redmod-2.0.0.jar (файл с "-sources" в названии не нужен).
Другу нужны: Minecraft 1.20.1, Fabric Loader, Fabric API. GeckoLib уже внутри .jar.

СБОРКА В INTELLIJ
Open -> эта папка, подожди загрузку Gradle, потом Gradle -> Tasks -> build -> build.
Запуск для теста: Gradle -> Tasks -> fabric -> runClient.

ГДЕ ЧТО ПОДГОНЯТЬ
- Размер обеих кнопок: ModEntities.java (MODEL_SCALE)
- Хитбоксы: ModEntities.java
- Прыжки, лечение, задержка нажатия: entity/RedButtonEntity.java (блок "Настройки" вверху)
- Здоровье большой: entity/RedButtonBigEntity.java
- Положение и размер шлема и факела: client/RedButtonRenderer.java
- Положение предмета в руке: assets/redmod/models/item/red_button_hand.json (раздел display)
- Модели, анимации, текстуры: src/main/resources/assets/redmod/...
