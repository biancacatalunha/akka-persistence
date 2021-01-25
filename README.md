# Akka Persistence

Problems with traditional databases:
- How do you query a previous state?
- How to trace how it arrived to the current state?

Instead of storing the *current state*, it stores *events*  
We can always recreate the current state by replaying events

## Event Sourcing

It is a new mental model

Pros:
- High performance: Events are only appended
- Avoids relational stores and ORM entirely
- Full trace of every state
- Fits the Akka actor model perfectly

Cons:
- Query a state is potentially expensive <- Akka Persistence Query
- Potential performance issues with long-lived entities (stream of events might be extremely large) <- Snapshotting
- Data model subject to change <- Schema evolution

## Persistent Actors
Can do everything a normal actor can do:
- Send and receive messages
- Hold internal state
- Run in parallel with many other actors

Extra capabilities:
- Have a persistence ID
- Send events to a long-term store
- Recover state by replaying events from the store

When an actor handles a ~~message~~ command:
- It can (asynchronously) persist an event to the store
- After the event is persisted, it changes its internal state

When an actor starts/restarts:
- It replays all events with the persistent id in the same other

## Course Details
Akka Persistence with Scala - Rock tge JVM - Udemy

### Contents
- [x] 57min Welcome
- [ ] 2h58min Akka Persistence Primer
- [ ] 1h22min Akka Persistence Stores and Serialization
- [ ] 1h48min Advanced Akka Persistence Patterns and Practices

