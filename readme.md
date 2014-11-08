# Timeseries zen

Long term goal: To be a better graphite. Not much here right now.

## Playing around

Suppose `/tmp/TSZFiles` is the directory where you store your data. Open an sbt console.

```scala
import com.timeserieszen.wal_handlers._
import com.timeserieszen.listener._
UDPListener(9999).map(x => {println(x); x}).to(new TextWALHandler("/tmp/TSZFiles").writer).run.run
```

This will make you listen on port 9999 for graphite like messages. One major difference - you can include *multiple* keys and values in the same message. To test this:

```bash
$ echo -n "foo.bar 23.3 foo.baz 25.3 12350" | nc -4u -w1 localhost 9999
```
