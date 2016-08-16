
play-slick-multitenant-seed
===========================


An attempt at a seed project for Play Framework 2.5 + Slick 3 (PostGres) for a MultiTenant Use Case. This repo is intended to help you quickly get up and running in building a fully asynchronous, microservices based restful service that can cater to multiple clients (multi tenancy) complete with Search, Logging, Email Service, Authorization built in.

As far as Play Framework's Dependancy Injection is concerned, i've decided to use compile time DI rather than runtime DI.  Also, Hikari will be used for database connection pooling.

> **Note:**

> - This project is far from stable, plenty of things to be done.
> - Feedback and contributions are welcome. :)

----------

Upcoming Features
-------------

##### <i class="icon-shield"></i> Authorization via Deadbolt
##### <i class="icon-mail"></i> Email Service
#####<i class="icon-flash"></i> Search Powered by ES
#####<i class="icon-list"></i> Logs propagated to GrayLogs 2/Logstash

----------

#####stay tuned