(ns riemann.plugin.samplerr-test
  (:require [clojure.test :refer :all]))

; (.format (.truncatedTo (java.time.ZonedDateTime/now) java.time.temporal.ChronoUnit/DAYS) java.time.format.DateTimeFormatter/ISO_DATE_TIME)
; (.format (.truncatedTo (java.time.ZonedDateTime/now (java.time.ZoneId/of "UTC")) java.time.temporal.ChronoUnit/DAYS) java.time.format.DateTimeFormatter/ISO_DATE_TIME)

; 16 june 2020, 15:27:00 (UTC-1000) aka 17 june 2020 01:27:00 (GMT)
(def test-date
  (java.time.ZonedDateTime/of 2020 6 16 15 27 00 0 (java.time.ZoneId/of "Pacific/Tahiti")))

(def keep-days 3)
(def keep-months 2)
(def keep-years 10)

(def samplerr-alias-prefix "samplerr-")
(def samplerr-index-prefix ".samplerr-")

(defn utc
  "Return a date in UTC"
  [date]
  (.withZoneSameInstant date (java.time.ZoneId/of "UTC")))

(deftest utc-test
  (is (= (java.time.ZonedDateTime/of 2020 6 17 1 27 0 0 (java.time.ZoneId/of "UTC")) (utc test-date))))

(defn truncate-to-day
  "Truncate a date to the beginning of the day"
  [date]
  (.truncatedTo date java.time.temporal.ChronoUnit/DAYS))

(deftest truncate-to-day-test
  (is (= (java.time.ZonedDateTime/of 2020 6 16 0 0 0 0 (java.time.ZoneId/of "Pacific/Tahiti")) (truncate-to-day test-date))))

(defn truncate-to-month
  "Truncate a date to the beginning of the month"
  [date]
  (.withDayOfMonth (truncate-to-day date) 1))

(deftest truncate-to-month-test
  (is (= (java.time.ZonedDateTime/of 2020 6 1 0 0 0 0 (java.time.ZoneId/of "Pacific/Tahiti")) (truncate-to-month test-date))))

(defn truncate-to-year
  "Truncate a date to the beginning of the year"
  [date]
  (.withMonth (truncate-to-month date) 1))

(deftest truncate-to-year-test
  (is (= (java.time.ZonedDateTime/of 2020 1 1 0 0 0 0 (java.time.ZoneId/of "Pacific/Tahiti")) (truncate-to-year test-date))))

(defn format-date
  [date]
  (.format date (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd'T'HH:mm:ssXX")))

(deftest format-date-test
  (is (= "2020-06-17T01:27:00Z" (format-date (utc test-date)))))

(defn daily-aliases-dates
  [date n]
  (map #(.minusDays (truncate-to-day (utc test-date)) %) (range n)))

(deftest daily-aliases-dates-test
  (is (= '("2020-06-17T00:00:00Z"
           "2020-06-16T00:00:00Z"
           "2020-06-15T00:00:00Z") (map #(format-date %) (daily-aliases-dates test-date keep-days)))))

(defn monthly-aliases-dates
  [date n]
  (map #(.minusMonths (truncate-to-month (utc test-date)) %) (range n)))

(deftest monthly-aliases-dates-test
  (is (= '("2020-06-01T00:00:00Z"
           "2020-05-01T00:00:00Z") (map #(format-date %) (monthly-aliases-dates test-date keep-months)))))

(defn yearly-aliases-dates
  [date n]
  (map #(.minusYears (truncate-to-year (utc test-date)) %) (range n)))

(deftest yearly-aliases-dates-test
  (is (= '("2020-01-01T00:00:00Z"
           "2019-01-01T00:00:00Z"
           "2018-01-01T00:00:00Z"
           "2017-01-01T00:00:00Z"
           "2016-01-01T00:00:00Z"
           "2015-01-01T00:00:00Z"
           "2014-01-01T00:00:00Z"
           "2013-01-01T00:00:00Z"
           "2012-01-01T00:00:00Z"
           "2011-01-01T00:00:00Z") (map #(format-date %) (yearly-aliases-dates test-date keep-years)))))

(defn year-alias-names
  [date]
  (.format date (java.time.format.DateTimeFormatter/ofPattern "'samplerr-'yyyy")))

(defn month-alias-names
  [date]
  (.format date (java.time.format.DateTimeFormatter/ofPattern "'samplerr-'yyyy.MM")))

(defn day-alias-names
  [date]
  (.format date (java.time.format.DateTimeFormatter/ofPattern "'samplerr-'yyyy.MM.dd")))

(defn allowed-aliases
  []
  (concat
    (map #(day-alias-names %) (daily-aliases-dates test-date keep-days))
    (map #(month-alias-names %) (monthly-aliases-dates test-date keep-months))
    (map #(year-alias-names %) (yearly-aliases-dates test-date keep-years))))

(deftest allowed-aliases-test
  (is (= '("samplerr-2020.06.17"
           "samplerr-2020.06.16"
           "samplerr-2020.06.15"
           "samplerr-2020.06"
           "samplerr-2020.05"
           "samplerr-2020"
           "samplerr-2019"
           "samplerr-2018"
           "samplerr-2017"
           "samplerr-2016"
           "samplerr-2015"
           "samplerr-2014"
           "samplerr-2013"
           "samplerr-2012"
           "samplerr-2011") (allowed-aliases))))

(def existing-aliases '("samplerr-2020.06.16" "samplerr-2020.06.15" "samplerr-2020.06.14" "samplerr-2020.06" "samplerr-2020.05" "samplerr-2020.04" "samplerr-2011" "samplerr-2010" "samplerr-2009"))

(defn deprecated-aliases
  [current-aliases allowed-aliases]
    (filter #(some? %) (map #(if (contains? allowed-aliases %) nil %) current-aliases)))

(deftest deprecated-aliases-test
  (is (= '("samplerr-2020.06.14"
            "samplerr-2020.04"
            "samplerr-2010"
            "samplerr-2009") (deprecated-aliases existing-aliases (set (allowed-aliases))))))

(defn delete-aliases
  [aliases]
  (map (fn [a]  { :remove { :index (str "." a) :alias a } } ) aliases))

(deftest delete-aliases-test
  (is (= '({:remove { :index ".samplerr-2009" :alias "samplerr-2009" }}) (delete-aliases '("samplerr-2009")))))

(defn- add-alias
  [date precision limit]
  (cond (= precision :day)
        { :add { :alias (str samplerr-alias-prefix (day-alias-names date)) :index (str samplerr-index-prefix (day-alias-names date)) }}
        (= precision :month)
        (if (.isAfter (.plusMonths date 1) limit)
          { :add { :alias (str samplerr-alias-prefix (month-alias-names date)) :index (str samplerr-index-prefix (month-alias-names date)) :range { "@timestamp" { :lt (format-date (truncate-to-day limit)) } } }}
          { :add { :alias (str samplerr-alias-prefix (month-alias-names date)) :index (str samplerr-index-prefix (month-alias-names date)) }})
        (= precision :year)
        (if (.isAfter (.plusYears date 1) limit)
          { :add { :alias (str samplerr-alias-prefix (year-alias-names date)) :index (str samplerr-index-prefix (year-alias-names date)) :range { "@timestamp" { :lt (format-date (truncate-to-month limit)) } } }}
          { :add { :alias (str samplerr-alias-prefix (year-alias-names date)) :index (str samplerr-index-prefix (year-alias-names date)) }})))

(defn add-samplerr-aliases
  [date keep-days keep-months keep-years]
  (let [da-dates (daily-aliases-dates date keep-days)
        ma-dates (monthly-aliases-dates date keep-months)
        ya-dates (yearly-aliases-dates date keep-years)
        first-day (last da-dates)
        first-month (last ma-dates)
        ]
    (concat
      (map #(add-alias % :day nil) da-dates)
      (map #(add-alias % :month first-day) ma-dates)
      (map #(add-alias % :year first-month) ya-dates))))

(deftest add-samplerr-aliases-test
  (is (= '({:add {:alias "samplerr-samplerr-2020.06.17", :index ".samplerr-samplerr-2020.06.17"}}
           {:add {:alias "samplerr-samplerr-2020.06.16", :index ".samplerr-samplerr-2020.06.16"}}
           {:add {:alias "samplerr-samplerr-2020.06.15", :index ".samplerr-samplerr-2020.06.15"}}
           {:add {:alias "samplerr-samplerr-2020.06", :index ".samplerr-samplerr-2020.06", :range {"@timestamp" {:lt "2020-06-15T00:00:00Z"}}}}
           {:add {:alias "samplerr-samplerr-2020.05", :index ".samplerr-samplerr-2020.05"}}
           {:add {:alias "samplerr-samplerr-2020", :index ".samplerr-samplerr-2020", :range {"@timestamp" {:lt "2020-05-01T00:00:00Z"}}}}
           {:add {:alias "samplerr-samplerr-2019", :index ".samplerr-samplerr-2019"}}
           {:add {:alias "samplerr-samplerr-2018", :index ".samplerr-samplerr-2018"}}
           {:add {:alias "samplerr-samplerr-2017", :index ".samplerr-samplerr-2017"}}
           {:add {:alias "samplerr-samplerr-2016", :index ".samplerr-samplerr-2016"}}
           {:add {:alias "samplerr-samplerr-2015", :index ".samplerr-samplerr-2015"}}
           {:add {:alias "samplerr-samplerr-2014", :index ".samplerr-samplerr-2014"}}
           {:add {:alias "samplerr-samplerr-2013", :index ".samplerr-samplerr-2013"}}
           {:add {:alias "samplerr-samplerr-2012", :index ".samplerr-samplerr-2012"}}
           {:add {:alias "samplerr-samplerr-2011", :index ".samplerr-samplerr-2011"}}) (add-samplerr-aliases test-date 3 2 10))))

(def aliases '("samplerr-2020.06.14" "samplerr-2020.04" "samplerr-2010" "logs-2020.06.14" "logs-2020.06.13"))

(defn samplerr-aliases
  "Filter aliases related to samplerr"
  [aliases samplerr-alias-prefix]
  (filter #(clojure.string/starts-with? % samplerr-alias-prefix) aliases))

(deftest samplerr-aliases-test
  (is (= '("samplerr-2020.06.14" "samplerr-2020.04" "samplerr-2010") (samplerr-aliases aliases samplerr-alias-prefix))))

; (delete-aliases (samplerr-aliases (all-aliases) "samplerr-"))
; (setup-aliases ...)
