# Slide code for my _Introduction to ZIO_ talk

This project contains the Scala code for some of the examples on the
slides for my presentation the Chicago Area Scala Enthusiasts (CASE)
on October 22, 2020: [Functional Effects with an Introduction to
ZIO](https://www.meetup.com/chicagoscala/events/273367960/). It will
likely not make any sense outside of that context. The slides are
available [here](https://slides.com/marcsaegesser/deck-de3999).

## Examples
There are two types of examples. The first are runnable programs and
rest are just expressions to be evaluated in the REPL.

To try the programs, just use `run` from the sbt prompt and select the
desired program. These include the progression of _Prompt_
applications that I used to introduce ZIO and some simple combinators.

The final application is a more fleshed out version of the
fork/join/interrupt example. This uses HTTPS to start an HTTP server
listening on localhost:8080. Point a browser to
http://localhost:8080/example/ to verify that it's working. The
example DataLoop will periodically output to the console. Use Ctrl-c
to stop the program and note that HTTPS terminates cleanly and
`DataLoop`'s release ZIO is performed.

The remaining examples can be tried using the REPL. After starting the
sbt console, run the following command to set up the environment.

```
:load setup
```

This will set up the necessary imports and create a value called 'r'
which contains the default ZIO Runtime. This simply because typing 'r' is
much easier than typing `Runtime.default` all the time.

For example, use this to run one of the `RetryRepeat` examples.

```scala
import RetryRepeat._
r.unsafeRunSync(b.repeat(expMax))
```
