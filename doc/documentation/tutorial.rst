.. _getting_started:

*************************
Quick Start Guide
*************************

In this guide, we will download Verdict, and issue analytic SQL queries to your
database using Verdict's command-line interface *veeline*. Note that Verdict
provides fast big data analytics as working on top of an existing database. In
this guide, we will set Verdict to connect to MySQL. Connecting to the other
databases follow the same instruction except for a connection string (as will be
described below).


Prerequisites
=====================

1. **Java Development Kit (JDK) 1.7 or above** You can download a recent version of JDK
   from `Oracle website
   <http://www.oracle.com/technetwork/java/javase/downloads/index.html>`_ or can
   install open JDK using package managers such as :code:`apt-get`.

2. **Apache Maven** The official instruction for installing Apache Maven is
   available from `Maven website
   <https://maven.apache.org/install.html>`_. Apache Maven is also available
   from many package managers.

3. **Supported Database** :ref:`features` page lists supported databases. In
   this guide, we will use MySQL since it is already installed in many operating
   systems. Using this guide for other databases only requires the change of
   connection string (which includes the name of your database, host address, etc.).


Download and Install
=====================

Download the zip file that includes source code and command-line interface from
the :ref:`download` page. Unzip the downloaded file and go to the directory.

Type the following command to compile the source code.

.. code-block:: bash

    bash$ mvn package

Successful compilation creates three jar files in the :code:`target` directory
as follows.

.. code-block:: bash

    bash$ ls -l target
    total 16000
    drwxr-xr-x   5 pyongjoo  staff   170B May 30 21:58 .
    drwxr-xr-x  14 pyongjoo  staff   476B May 30 20:24 ..
    -rw-r--r--   1 pyongjoo  staff   3.7M May 30 21:53 verdict-core-0.2.0-SNAPSHOT-jar-with-dependencies.jar
    -rw-r--r--   1 pyongjoo  staff   3.7M May 30 21:53 verdict-jdbc-0.2.0-SNAPSHOT-jar-with-dependencies.jar
    -rw-r--r--   1 pyongjoo  staff   379K May 30 21:53 verdict-veeline-0.2.0-SNAPSHOT-jar-with-dependencies.jar


Preparing Test Data
====================================

We will create a test table in MySQL for this guide. Suppose MySQL is installed
locally, and its username/password is verdict/verdict, and the user :code:`verdict`
has read/write access to the database schema :code:`test`.

Go to the :code:`veeline` directory, and run the :code:`gen_test_table.sql`
script by typing:

.. code-block:: bash

    bash$ mysql -u verdict -pverdict test < gen_test_table.sql

The script creates a table named :code:`test_table`. The table follows a
schema :code:`(name INT, value DOUBLE)`. The script also populates about 8
million randomly generated tuples into the table.


Connecting to Database using Veeline
====================================

Veeline is a command-line interface for Verdict. In the 'veeline' directory,
type the below command to start veeline and connect to the MySQL database.

.. code-block:: bash

    bash$ bin/veeline -h mysql://<host addrses>[:<port>] -u <username> -p <password>

For other databases, a different database name should be used after the
:code:`-h` argument. :ref:`veeline` page lists the names for other databases. If the port number is
not specified, Verdict uses a database-specific default port number. (The default port numbers are
set in the :code:`core/src/main/resources/default.conf` file).
If MySQL is installed locally, and its username/password is
verdict/verdict, one can start veeline by typing:

.. code-block:: bash

    bash$ bin/veeline -h mysql://localhost:3306 -u verdict -p verdict

This command will display a prompt :code:`verdict:MySQL>`.


Basic Veeline Commands
=======================

Choose the :code:`test` database by typing:

.. code-block:: bash

    verdict:MySQL> use test;

You can list tables in the database as follows.

.. code-block:: bash

    verdict:MySQL> show tables;
    +-------------+
    | TABLE_NAME  |
    +-------------+
    | test_table  |
    +-------------+
    1 row selected (0.014 seconds)

You can also display the table definition as follows.

.. code-block:: bash

    verdict:MySQL> describe test_table;
    +--------------+------------+----------+
    | COLUMN_NAME  | TYPE_NAME  | REMARKS  |
    +--------------+------------+----------+
    | name         | INT        |          |
    | value        | DOUBLE     |          |
    +--------------+------------+----------+
    2 rows selected (0.014 seconds)


Sample Creation
=====================

Verdict speeds up its query processing by using sample tables. To create a
sample of the :code:`test_table` table, type:

.. code-block:: bash

    verdict:MySQL> create sample from test_table;
    INFO   2017-05-30 23:01:37,578 - [VerdictCreateSampleQuery] Create a 1.0000 percentage sample of test_table.
    1 row affected (12.483 seconds)

By default, :code:`create sample` query creates 1% sample. :ref:`features` page lists
more options.


Approximate Analysis
=====================

We will first issue two queries through Verdict. Next, we will compare those
approximate answers by Verdict to the exact answers. For this guide, we will use
relatively simple aggregate queries; however, Verdict supports complex nested
queries as presented :ref:`examples` page.

The first query is a groupby-count query.

.. code-block:: bash

    verdict:MySQL> select name, count(*) from test_table group by name;
    INFO   2017-05-30 23:03:14,194 - Verdict is using a sample table for test.test_table
    +-------+--------------------+
    | name  | count(*) (Approx)  |
    +-------+--------------------+
    | 1     | 1051620            |
    | 2     | 2101434            |
    | 3     | 3137097            |
    | 4     | 4184703            |
    | 5     | 5253785            |
    +-------+--------------------+
    5 rows selected (0.09 seconds)


The second query is an groupby-average query.

.. code-block:: bash

    verdict:MySQL> select name, avg(value) from test_table group by name;
    INFO   2017-05-30 23:04:23,088 - Verdict is using a sample table for test.test_table
    +-------+----------------------+
    | name  | avg(value) (Approx)  |
    +-------+----------------------+
    | 1     | 526.8445440930807    |
    | 2     | 986.6722179157971    |
    | 3     | 1020.0752693151406   |
    | 4     | 589.3834650199314    |
    | 5     | 1958.1851174049732   |
    +-------+----------------------+
    5 rows selected (0.105 seconds)

On average, Verdict took about 0.1 second for answering those queries. Now let
us compare these to the exact results. To disable Verdict's approximate
processing, we set a :code:`bypass` option to true as follows:

.. code-block:: bash

    verdict:MySQL> set bypass='true';
    +-----------+-------------+
    | conf_key  | conf_value  |
    +-----------+-------------+
    | bypass    | true        |
    +-----------+-------------+
    1 row selected (0.006 seconds)

Now let us issue those two queries again.

.. code-block:: bash

    verdict:MySQL> select name, count(*) from test_table group by name;
    INFO   2017-05-30 23:07:16,484 - Verdict bypasses this query. Run "set bypass='false'" to enable Verdict's approximate query processing.
    +-------+-----------+
    | name  | count(*)  |
    +-------+-----------+
    | 1     | 1048576   |
    | 2     | 2097152   |
    | 3     | 3145728   |
    | 4     | 4194304   |
    | 5     | 5242880   |
    +-------+-----------+
    5 rows selected (7.725 seconds)
    verdict:MySQL> 
    verdict:MySQL> select name, avg(value) from test_table group by name;
    INFO   2017-05-30 23:07:34,208 - Verdict bypasses this query. Run "set bypass='false'" to enable Verdict's approximate query processing.
    +-------+---------------------+
    | name  |     avg(value)      |
    +-------+---------------------+
    | 1     | 532.5015406562342   |
    | 2     | 958.4139427125336   |
    | 3     | 1029.2677763141228  |
    | 4     | 590.2856817643471   |
    | 5     | 1950.7577719997025  |
    +-------+---------------------+
    5 rows selected (7.838 seconds)

In this example, Verdict processed those queries about 77 times faster. The
answers by Verdict were still 99% accurate. Verdict can show even bigger
speedups when the original data are larger.


Exiting Veeline
=====================

You can exit veeline by typing :code:`!quit`.



Learn More
=====================

We describe more veeline options in the :ref:`veeline` page.

Your application can connect to Verdict programmatically using the standard JDBC
interface provided by Verdict (see :ref:`jdbc` page).

Verdict supports secure access to your database using Kerberos and SSL. See
:ref:`secure` page.

If you want to understand the architecture of Verdict, see :ref:`architecture`
page.


