* OAuth? [here's a java one](http://code.google.com/p/oauth-signpost/). Just filter through cat app?
* Version (it should know, like kestrel does)
* Configuration
* Rules for achievements or whatever

{
  "query": {
    "wildcard": {
      "action": "goal-progress"
    }
  },
  "filter": {
    "and": [
      {
        "term": {
          "person.id": 2
        }
      },
      {
        "range": {
          "timestamp": {
            "gte": "2012-03-21T19:01:01"
          }
        }
      }
    ]
  },
  "facets": {
    "actions": {
      "terms": {
        "field": "timestamp"
      },
      "facet_filter": {
        "and": [
          {
            "term": {
              "person.id": 2
            }
          },
          {
            "range": {
              "timestamp": {
                "gte": "2012-03-21T19:01:01"
              }
            }
          }
        ]
      }
    }
  }
}