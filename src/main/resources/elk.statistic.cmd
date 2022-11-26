DELETE statistic

PUT statistic/
{
  "mappings" : {
    "properties" : {
      "cpu" : {
        "type" : "float"
      },
      "timestamp" : {
        "type" : "date"
      }
    }
  }
}

GET _search
{
  "query": {
    "match_all": {}
  }
}

GET statistic/_mapping

GET statistic/_search