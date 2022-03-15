# Avandy news founder


Поисковик новостей по RSS потокам популярных новостных источников (*Открыто для доработок! open source*). 

Особенность программы в удобном, автоматическом поиске новостей по нескольким ключевым словам с отправкой результатов поиска на электронную почту.

Актуальная версия прграммы: https://github.com/mrprogre/news_lite/raw/master/News.jar

Для работы приложения на ПК должна быть установлена Java: https://www.java.com/ru/download/

----

<b>Актуальная версия интерфейса:</b>

![image](https://github.com/mrprogre/news_lite/blob/master/gui.png)


----
**Возможности программы:**
- поиск по ключевому слову
- поиск по ключевым словам. Слова можно удалять, а вместо них добавлять новые (без ограничений)
- автоматическое обновление данных для основного поиска или поиска по ключевым словам каждые 60 секунд (посредством установки чекбокса)
- отправка результата поиска на электронную почту (+ возможность выбора автоматической отправки результатов после каждого поиска)
- экспорт результатов в Excel
- двойной щелчок по заголовку открывает веб-страницу новости
- анализ частоты употребления слов в заголовках новостей с использованием базы данных SQLite (отображает всё, что повторяется > 2 раз)
- отображение журнала работы программы
- при закрытии программы сохраняются все установленные чекбоксы, адрес электронной почты, ключевые слова и интервал поиска
- возможность работы в базе SQLite
- возможность добавления нового источника RSS. Деактивация существующих или их удаление.
- возможность просмотра папки с файлами программы
- возможность исключить ненужные слова из таблицы анализа частоты употребления слов (и вернуть их обратно из исключенных)
- в файле config.txt после ключа "exclude" можно указать 3 слова, которые нас не интересуют изначально (удалять их или оставлять пустыми нельзя)
- программу можно вызывать в консоли Windows или Linux без интерфейса. Примеру: jar выкладывается на сервер где установлен Linux и указывается команда в Сrontab для круглосуточного вызова программы. Ни одна новость не будет упущена! Вот команда, которую указывал я (параметр № 1 - адресат, 2 - интервал (момент вызова минус 1440 минут (сутки), далее ключевые слова через пробел. Т.е. каждый день в 10:30 утра я получаю все новости за сутки по указанным словам + веду лог работы программы):

        30 10 * * * java -jar /home/user/news.jar user@mail.ru 1440 выплат ипотек рефинанс штраф налог > /home/user/news.log

В Windows при вызове данной команды в PowerShell:

        java -jar .\news.jar user@mail.ru 180 москв хлеб балаших
        
![image](https://github.com/mrprogre/news_lite/blob/master/power-shell.png)

----
Пароль и почту, с которой будет идти отправка, необходимо указывать в файле config.txt после ключей "from_pwd" и "from_adr" соответственно

Путь к файлу настроек в Windows: C:\Users\<user>\News

В Linux: home/<user>/News

Отправка производится с почтовых сервисов: Mail.ru, Gmail, Yandex, Yahoo, Rambler.


----
Работа в приложении выглядит следующим образом (интерфейс старый):

![Image alt](https://github.com/mrprogre/news_lite/blob/master/gui.gif)

----
Search for all news in popular RSS lists:
        
Forbes, Yandex, Mail.ru, Lenta, Izvestiya, Vesti, Garant, Life, RBK, Gazeta.ru, Vedomosti, BBC, Poplulyarnaya mekhanika, 
Nauka i zhizn', RT, C-main.Main, Pravitel'stvo RF, Gosduma RF, Rossiyskaya gazeta, Rosteh, Kommersant, Expert, Nasa, 
Yandex.Kosmos, Astronews, Google, Finam, Aton, MMVB, Esquire, GQ, N+1, SmartLab        
        
Program features:
- keyword search
- keyword search. Words can be deleted, and new ones can be added instead (no restrictions)
- automatic update of data for the main search or search by keywords every 60 seconds (by setting the checkbox)
- sending the search result to e-mail (+ the ability to choose the automatic sending of results after each search)
- export results to Excel
- double click on the title opens the news webpage
- analysis of the frequency of words in news headlines using the SQLite database (displays everything that is repeated > 2 times)
- displaying the log of the program
- when closing the program, all set checkboxes, email address, keywords and search interval are saved
- the ability to work in the SQLite database
- Ability to add a new RSS source. Deactivate existing ones or delete them.
- the ability to view the folder with the program files
- the ability to exclude unnecessary words from the word frequency analysis table (and return them back from the excluded ones)
- in the config.txt file, after the "exclude" key, you can specify 3 words that we are not initially interested in (you cannot delete them or leave them empty)
- jar can be uploaded to the server where Linux is installed and specify the command in Сrontab to call the program around the clock. No news will be missed! Here is the command that I indicated (parameter No. 1 - the addressee, 2 - the interval (the moment of the call minus 1440 minutes (day), then the keywords. That is, every day at 10:30 am I received all the news for the day for the specified words + wrote them to the log)       
- work without UI with commands (in Linux-Crontab or Windows-PowerShell) like:

      30 10 * * * java -jar /home/user/news.jar user@mail.ru 1440 amazon apple tax> /home/user/news.log
