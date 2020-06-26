(ns riemann.plugin.samplerr-test
  (:require [clojure.test :refer :all]))

(require '[riemann.plugin.samplerr :as samplerr])

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

(deftest utc-test
  (is (= (java.time.ZonedDateTime/of 2020 6 17 1 27 0 0 (java.time.ZoneId/of "UTC")) (samplerr/utc test-date))))

(deftest truncate-to-day-test
  (is (= (java.time.ZonedDateTime/of 2020 6 16 0 0 0 0 (java.time.ZoneId/of "Pacific/Tahiti")) (samplerr/truncate-to-day test-date))))

(deftest truncate-to-month-test
  (is (= (java.time.ZonedDateTime/of 2020 6 1 0 0 0 0 (java.time.ZoneId/of "Pacific/Tahiti")) (samplerr/truncate-to-month test-date))))

(deftest truncate-to-year-test
  (is (= (java.time.ZonedDateTime/of 2020 1 1 0 0 0 0 (java.time.ZoneId/of "Pacific/Tahiti")) (samplerr/truncate-to-year test-date))))

(deftest format-date-test
  (is (= "2020-06-17T01:27:00Z" (samplerr/format-date (samplerr/utc test-date)))))

(deftest daily-aliases-dates-test
  (is (= '("2020-06-17T00:00:00Z"
           "2020-06-16T00:00:00Z"
           "2020-06-15T00:00:00Z") (map #(samplerr/format-date %) (samplerr/daily-aliases-dates test-date keep-days)))))

(deftest monthly-aliases-dates-test
  (is (= '("2020-06-01T00:00:00Z"
           "2020-05-01T00:00:00Z") (map #(samplerr/format-date %) (samplerr/monthly-aliases-dates test-date keep-months)))))

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
           "2011-01-01T00:00:00Z") (map #(samplerr/format-date %) (samplerr/yearly-aliases-dates test-date keep-years)))))

(deftest remove-aliases-test
  (is (= '({:remove { :index ".samplerr-2009" :alias "samplerr-2009" }}) (samplerr/remove-all-aliases '("samplerr-2009")))))


;(deftest add-samplerr-aliases-test
;  (is (= '({:add {:alias "samplerr-samplerr-2020.06.17", :index ".samplerr-samplerr-2020.06.17"}}
;           {:add {:alias "samplerr-samplerr-2020.06.16", :index ".samplerr-samplerr-2020.06.16"}}
;           {:add {:alias "samplerr-samplerr-2020.06.15", :index ".samplerr-samplerr-2020.06.15"}}
;           {:add {:alias "samplerr-samplerr-2020.06", :index ".samplerr-samplerr-2020.06", :range {"@timestamp" {:lt "2020-06-15T00:00:00Z"}}}}
;           {:add {:alias "samplerr-samplerr-2020.05", :index ".samplerr-samplerr-2020.05"}}
;           {:add {:alias "samplerr-samplerr-2020", :index ".samplerr-samplerr-2020", :range {"@timestamp" {:lt "2020-05-01T00:00:00Z"}}}}
;           {:add {:alias "samplerr-samplerr-2019", :index ".samplerr-samplerr-2019"}}
;           {:add {:alias "samplerr-samplerr-2018", :index ".samplerr-samplerr-2018"}}
;           {:add {:alias "samplerr-samplerr-2017", :index ".samplerr-samplerr-2017"}}
;           {:add {:alias "samplerr-samplerr-2016", :index ".samplerr-samplerr-2016"}}
;           {:add {:alias "samplerr-samplerr-2015", :index ".samplerr-samplerr-2015"}}
;           {:add {:alias "samplerr-samplerr-2014", :index ".samplerr-samplerr-2014"}}
;           {:add {:alias "samplerr-samplerr-2013", :index ".samplerr-samplerr-2013"}}
;           {:add {:alias "samplerr-samplerr-2012", :index ".samplerr-samplerr-2012"}}
;           {:add {:alias "samplerr-samplerr-2011", :index ".samplerr-samplerr-2011"}}) (add-samplerr-aliases test-date 3 2 10))))
