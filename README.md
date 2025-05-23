# **Отчет о реализации пользовательского пула потоков**

Пример запуска приложения описан в файле logs.txt

## Обзор

Реализация предоставляет пользовательский пул потоков с расширенными функциями, включая распределение задач Round Robin, пользовательскую фабрику потоков и подробное ведение журнала. Пул поддерживает основные и максимальные размеры пула, время поддержания активности, минимальное количество резервных потоков и пользовательский обработчик отклонений.

## Производительность

По сравнению с ThreadPoolExecutor Java:

* Похожая производительность для выполнения базовой задачи

* Лучший контроль над управлением потоками благодаря minSpareThreads

* более высокие накладные расходы из-за нескольких очередей, но лучшее распределение задач

* Более подробное ведение логирования

## Рекомендуемые параметры:

* corePoolSize=2, maxPoolSize=4: хороший баланс для умеренных рабочих нагрузок

* queueSize=5: предотвращает чрезмерное использование памяти, допуская буферизацию

* keepAliveTime=5s: разумно для очистки ресурсов без преждевременного завершения потока

## Распределение задач

Реализация использует распределение Round Robin по нескольким очередям:

Каждый основной поток имеет свою собственную очередь

Задачи назначаются очередям циклическим образом

+: предотвращает превращение любой отдельной очереди в узкое место

-: не всегда может назначать задачи наименее загруженной очереди

## Обработка отклонений

Пользовательский обработчик отклонений выдает RejectedExecutionException, когда очереди заполнены и новые потоки не могут быть созданы:

+: простота, предотвращает неограниченный рост

-: требуется клиентский код для обработки отклонений

## Рекомендации

Для задач, привязанных к ЦП: установите corePoolSize близко к доступным ядрам ЦП

Для задач, привязанных к вводу-выводу: увеличьте maxPoolSize и queueSize

Отслеживайте показатели отклонений для настройки queueSize

Отрегулируйте keepAliveTime на основе частоты задач