(set-env!
  :source-paths #{"src"}
  :resource-paths #{"resources"}
  :dependencies '[[hiccup "1.0.5"]
                  [perun "0.1.3-SNAPSHOT"]
                  [hashobject/boot-s3 "0.1.0-SNAPSHOT"]
                  [clj-time "0.9.0"]
                  [pandeiro/boot-http "0.6.2"]
                  [pandeiro/boot-http "0.6.3-SNAPSHOT"]
                  [org.martinklepsch/boot-gzip "0.1.1"]])


(require '[io.perun :refer :all]
         '[blog.hashobject.views.index :as index-view]
         '[blog.hashobject.views.post :as post-view]
         '[pandeiro.boot-http :refer [serve]]
         '[hashobject.boot-s3 :refer :all]
         '[org.martinklepsch.boot-gzip :refer [gzip]])


(task-options!
  pom {:project 'blog.hashobject.com
       :version "0.2.0"}
  s3-sync {
    :bucket "blog.hashobject.com"
    :source "resources/public/"
    :options {"Cache-Control" "max-age=315360000, no-transform, public"}})

(deftask build
  "Build blog."
  [d dev  bool "Don't build rss, sitemap, deploy to S3"]
  (comp (markdown)
        (draft)
        (ttr)
        (slug)
        (permalink)
        (render :renderer 'blog.hashobject.views.post/render)
        (collection :renderer 'blog.hashobject.views.index/render :page "index.html")
        (if dev identity (sitemap :filename "sitemap.xml"))
        (if dev identity (rss :title "Hashobject" :description "Hashobject blog" :link "http://blog.hashobject.com"))
        (gzip :regex [#".html$" #".css$" #".js$"])
        (if dev identity (s3-sync :access-key (System/getenv "AWS_ACCESS_KEY")
                                  :secret-key (System/getenv "AWS_SECRET_KEY")))))

(deftask dev
  []
  (comp (watch)
        (build :dev true)
        (serve :resource-root "public")))