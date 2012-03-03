# Tyler

Tyler is a web service that tracks the actions of a user.  It keeps counts
of the actions performed, a timeline of recent events and keeps track of
achievements the user has earned.

## REST

Tyler is REST. Yeaaaaah.

## Details On Actions

Action name should stick to a-zA-Z0-9\-_

### Create An Action

New actions will be vivified if they do not exist, incremented if they do.

    curl -v -XPOST 'http://127.0.0.1:8080/user/1/action' -d '{"name":"action-name"}' -H "Content-Type: application/json"

The data posted **must** have a name key.  This should follow the aforementioned action name restrictions.

### Retrieving All Actions (and counts)

    curl -v 'http://127.0.0.1:8080/user/1/actions' -H "Content-Type: application/json"

### Retrieving Count Of A Specific Action

    curl -v 'http://127.0.0.1:8080/user/1/actions?search=$actionname' -H "Content-Type: application/json"
    
Accepts wildcards

    curl -v 'http://127.0.0.1:8080/user/1/actions?search=$actio*' -H "Content-Type: application/json"
    
Returns a map of action names and counts, like this:

    {
      "action-name": "1",
      "other-action-name": "2"
    }

### Retrieving A User's Timeline
    
    curl -v 'http://127.0.0.1:8080/user/1/timeline' -H "Content-Type: application/json"

### Adding To The Timeline

New actions will be vivified if they do not exist, incremented if they do.

    curl -v -XPOST 'http://127.0.0.1:8080/user/1/timeline' -d '{"name":"action-name"}' -H "Content-Type: application/json"

### Deleting A User

    curl -v -XDELETE 'http://127.0.0.1:8080/user/1' -H "Content-Type: application/json"

## Internal Stats

* stats/add/action: Count of action adds

* stats/add/timeline: Count of timeline adds

* stats/get/action: Count of action gets

* stats/get/timeline: Count of timeline gets

* stats/purge/user: Count of user purges

## Key Names

User Action counters: `user/:userid/action-count/:actionname`
Timeline: `user/:userid/timeline`

User Timeline: user/:userid/timeline

# Running

Launch [SBT](http://code.google.com/p/simple-build-tool).

        ./sbt

Run Jetty

        container:start

Go to http://localhost:8080/.
