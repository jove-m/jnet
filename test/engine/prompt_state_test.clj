(ns engine.prompt-state-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [engine.prompt-state :as sut]))

(deftest make-prompt-state-test
  (testing "validation works"
    (is (thrown? java.lang.AssertionError (sut/make-prompt-state :player)))))

(deftest set-prompt-test
  (testing "validation works"
    (is (thrown? java.lang.AssertionError
                 (-> (sut/make-prompt-state :corp)
                     (sut/set-prompt nil))))
    (is (thrown? java.lang.AssertionError
                 (-> (sut/make-prompt-state :corp)
                     (sut/set-prompt {:header "optional"})))))
  (is (= {:select-card true
          :header "header"
          :text "text"
          :buttons [:a :b :c]}
         (-> (sut/make-prompt-state :corp)
             (sut/set-prompt {:select-card true
                              :header "header"
                              :text "text"
                              :buttons [:a :b :c]})
             (select-keys [:select-card :header :text :buttons])))))

(deftest clear-prompt-test
  (is (= {:player :corp
          :select-card false
          :header ""
          :text ""
          :buttons []}
         (-> (sut/make-prompt-state :corp)
             (sut/clear-prompt)
             (select-keys [:player :select-card :header :text :buttons])))))
