# org.gjt.cuspy code originally on the Giant Java Tree

Way back [in 1999][wbgjt], when [Java 1.2][j12] was but five weeks old,
[git][] would not be born for six more years and [github][] for three more
after that, [Tim Endres][time] created an online hub for publication of
open-source Java software, based on (wait for it) [CVS][]: The
[Giant Java Tree][gjtdb].

[wbgjt]: http://web.archive.org/web/19990117014906/http://www.gjt.org/
[j12]: https://en.wikipedia.org/wiki/Java_version_history#J2SE_1.2
[git]: https://git-scm.com/book/en/v2/Getting-Started-A-Short-History-of-Git
[github]: http://web.archive.org/web/20080514210148/http://github.com/
[time]: http://www.trustice.com/index.shtml
[CVS]: http://web.archive.org/web/19991001210225/http://www.gjt.org/info/philosophy/cvsreasons.shtml
[gjtdb]: http://web.archive.org/web/19990910074206/http://www.gjt.org/info/dev/join/benefits.shtml

I had stuff there, too: in the package root [org.gjt.cuspy][cuspyjdoc]. At
the time I write this, those static Javadoc pages still work at `gjt.org`,
even though its CVS server and dynamic pages have been dark for years now.

[cuspyjdoc]: http://www.gjt.org/javadoc/org/gjt/cuspy/package-summary.html

Most of it is of strictly blast-from-the-past value any more. Much of it
was anticipating features that later officially appeared in Java, making
these things obsolete.

* Chained exceptions appeared in [Java 1.4][j14], obsoleting `Rethrown`
* My [RFE for named unicode characters][nuc] was `Closed Won't Fix` 17 months
    after submission, and then Unicode character names
    [appeared in Java 7][cgetname] ten years later. You still can't use the
    names in string literals, though it's been
    [possible in Perl since 2005][perlcn].
* `Interned` was my effort at making other things as easy to intern as
    `String`s. It was first used for:
* `ByteString` with which I advocated two strictly separate ways to program,
    either in the character domain or the byte domain, with equivalent library
    support for both, and the second style called _provincial-safe_. I do not
    see that Java has gone there, but it pleased me to see just such a design
    appear in Python 3. (Independently, as far as I can tell; there is no
    indication they got the idea from me, and they don't use the term
    _provincial-safe_.)
* `JarX` was a dead-easy way to solve a common problem of distributing archives
    with text and non-text files and getting them not extracted wrong regardless
    of the platform. Its related [Java bug report][ctbug] went unreviewed for
    nearly seven years, then was `Closed Won't Fix` with the comment

    > specification has been unchanged in this area for many years now, for
    > better for worse, and so it is too late to consider the changes suggested.

    And still to this day, things get distributed in a hodge-podge of
    `.tar.gz` or `.zip` formats according to hoary assumptions of which is
    more welcome on what platform, and even just getting the line endings in
    text files right still takes too much thinking....

Well, it can be fun to see some vestiges from the early Java days, anyway.

[j14]: https://en.wikipedia.org/wiki/Java_version_history#J2SE_1.4
[cgetname]: http://docs.oracle.com/javase/7/docs/api/java/lang/Character.html#getName(int)
[nuc]: http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4311312
[perlcn]: http://web.archive.org/web/20050404033102/http://perldoc.perl.org/charnames.html
[ctbug]: http://bugs.java.com/bugdatabase/view_bug.do?bug_id=4310708

## The CVS to git conversion

Because `org.gjt.cuspy` was (as the original docs said)

> not so much a single package as a home for useful standalone classes
> too small to get packages of their own

there is a branch in this repo for each one, which can be cloned to get only
that component, plus the `gjt` branch in which they all appear together as
they did on the Giant Java Tree ... plus the `master` branch where anything
new (like this README) is added, to preserve `gjt` as it was back when.

## Copying

Following the original [GJT licensing policy][gjtlp], these bagatelles
have always been public domain.

[gjtlp]: http://web.archive.org/web/19991002063520/http://www.gjt.org/info/policy/license/policy.shtml
