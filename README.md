# Tyler

Tyler is a web service that tracks the actions of a user.  It keeps counts
of the actions performed, a timeline of recent events and keeps track of
achievements the user has earned.

## REST

Tyler is REST. Yeaaaaah.

### Create An Action

New actions will be vivified if they do not exist, incremented if they do.

    curl -v -XPOST 'http://127.0.0.1:8080/user/1/action' -d '{"action":"action-name"}' -H "Content-Type: application/json"

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

# Data Structures

## Progress On A Goal

    {
        action    : 'goal-progress',
        timestamp : '2012-03-14 14:21:17',
        person_id : 1,      // ID of the user
        public    : {       // Subset of data that will be publicly visible
            action    : 'goal-progress',
            timestamp : '2012-03-14 14:21:17',
            person_id : 1,        // ID of the user
            goal_id   : 123,      // The ID for the goal,
            id        : 1726,     // The ID for the object in question
            quantity  : 1,        // How many?
        }
        goal_id   : 123,      // The ID for the goal,
        id        : 1726,     // The ID for the object in question
        quantity  : 1,        // How many?
        remaining : 0,        // Any remaining work (only relevant for progress?)
        text      : 'did it', // Any text note attached.
        params    : [ ], // Params for i18n? Just future proofing.
    }

## Adding A New Goal

    {
        action    : 'goal-added',
        timestamp : '2012-03-14 14:21:17',
        person_id : 1,      // ID of the user
        public    : {       // Subset of data that will be publicly visible
            action      : 'goal-added',
            timestamp   : '2012-03-14 14:21:17',
            person_id   : 1,        // ID of the user
            goal_id     : 124,      // The ID for the goal,
            id          : 124,      // The ID for the object in question (a goal)
            quantity    : 1,        // How many?
        }
        goal_id   : 124,      // The ID for the goal,
        id        : 124,      // The ID for the object in question (a goal)
        quantity  : 1,        // How many?
        remaining : 1,        // Won't really be used
        text      : 'Name of the goal', // Kinda lame?
        params    : [ ], // Params for i18n? Just future proofing.
    }

# Running

Launch [SBT](http://code.google.com/p/simple-build-tool).

        ./sbt

Run Jetty

        container:start

Go to http://localhost:8080/.
