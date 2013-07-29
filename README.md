ZooCreeper
==========

This project provides a command-line utility which can be used to back up
ZooKeeper data to a JSON file. It supports the following features:

*   Ignores ephemeral nodes when backing up by default.
*   Able to ignore ZooKeeper paths using '--exclude' regular expressions.
*   Optional compression of the backup file using GZIP.

To see the available options, run:

    $ java -jar zoocreeper-<VERSION>.jar --help

The only required option is '-z'/'--zk-connect' which is a standard
ZooKeeper connection string.

Also included is a BASH helper script:

    $ zoocreeper dump
