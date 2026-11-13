#!/bin/sh
# One entry point for everything that runs.  Usage: ./scasp.sh <command> [args]
#   web                        start the whole platform (web UI + TCP gateway + UDP notices + RMI admin)
#   gateway [host] [port]      interactive TCP client
#   udp [port]                 print UDP notifications
#   admin <host> <port> <ping|capacity|waiting|workload|audit|status> [args]   RMI admin client
#   race-demo                  Stage 2 race-condition evidence
#   benchmark [dumpfile]       platform vs virtual threads (optional thread dump file)
#   thread-dump                print a thread dump
cd "$(dirname "$0")"
CP="lib/*:out/classes"
cmd="$1"; [ $# -gt 0 ] && shift
case "$cmd" in
  web) exec java -cp "$CP" scasp.web.WebServer "$@" ;;
  gateway) exec java -cp "$CP" scasp.network.GatewayClient "$@" ;;
  udp) exec java -cp "$CP" scasp.network.UdpNotifier "$@" ;;
  admin) exec java -cp "$CP" scasp.rmi.AdminClient "$@" ;;
  race-demo) exec java -cp "$CP" scasp.processing.RaceDemo ;;
  benchmark) exec java -cp "$CP" scasp.processing.ProcessorBenchmark "$@" ;;
  thread-dump) exec java -cp "$CP" scasp.processing.ThreadDump ;;
  *) sed -n "2,12p" "$0" ;;
esac
