# Avandy News (Jdbc, Swing, SQLite)

Поисковик новостей по RSS популярных новостных источников

Download: https://github.com/mrprogre/news_lite/raw/master/news.jar

Download Java: https://www.java.com/download/

![image](https://user-images.githubusercontent.com/45883640/194651383-a29f8349-16f8-44b3-981a-7bb2b9b60a18.png)

Особенность программы в удобном, автоматическом поиске новостей по нескольким ключевым словам с отправкой результатов поиска на электронную почту.

Актуальная версия прграммы: https://github.com/mrprogre/news_lite/raw/master/news.jar

Для работы приложения на ПК должна быть установлена Java: https://www.java.com/ru/download/

----
**Возможности программы:**
- поиск заголовков по одному слову
- поиск по ключевым словам
- автоматическое обновление данных для основного поиска или поиска по ключевым словам каждые 60 секунд (посредством установки чекбокса)
- отправка результата поиска на электронную почту (+ возможность выбора автоматической отправки результатов после каждого поиска)
- экспорт результатов в Excel
- двойной щелчок по заголовку открывает веб-страницу новости
- анализ частоты употребления слов в заголовках новостей с использованием базы данных SQLite (отображает всё, что повторяется > 2 раз)
- отображение журнала работы программы
- сохранение состояния интерфейса
- возможность работы в SQLite
- возможность добавления нового источника RSS. Деактивация существующих или их удаление.
- возможность просмотра папки с файлами программы
- возможность исключить ненужные слова из таблицы анализа частоты употребления слов (и вернуть их обратно из исключенных)
- комбинацией ctrl+i в поле find добавляются слова, и заголовки, содержащие их, не будут отображаться
- программа работает в консоли
- перевод английских заголовков
- изучение английских слов (ну вот захотелось добавить :)
- настройка прозрачности окна
- сохранение избранных заголовков
- быстрый переход на часто используемые сайты (карты, поисковик и т.д.)

*Пример: jar выкладывается на сервер где установлен Linux и указывается команда в crontab для круглосуточного вызова программы. Ни одна новость не будет упущена! Вот команда, которую указывал я (параметр № 1 - адресат, 2 - интервал (момент вызова минус 1440 минут (сутки), далее ключевые слова через пробел. Т.е. каждый день в 10:30 утра я получаю все новости за сутки по указанным словам + веду лог работы программы):*

        30 10 * * * java -jar /home/user/news.jar user@mail.ru 1440 выплат ипотек рефинанс штраф налог >> /home/user/news.log

*В Windows при вызове команды в PowerShell:*

        java -jar .\news.jar user@mail.ru 180 москв хлеб балаших
        
![image](https://user-images.githubusercontent.com/45883640/188851087-8cdc2147-59f9-4d1e-8a3d-242adb972f41.png)

----
*Пароль и почту, с которой будет идти отправка, необходимо указывать в файле config.txt после ключей "from_pwd" и "from_adr" соответственно*

Путь к файлу настроек 

**Windows**: C:\Users\user\News\config.txt

**Linux**: home/user/News/config.txt

*Отправка производится с почтовых сервисов: Mail.ru, Gmail, Yandex, Yahoo, Rambler.*
