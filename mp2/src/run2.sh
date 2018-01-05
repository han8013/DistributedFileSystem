# javac UDPSever.java Member.java MembershipManagement.java Request.java SendThread.java \
#     Utils.java && java UDPSever Member MembershipManagement Request SendThread Utils
javac MP2Server.java && java MP2Server $(ifconfig | grep -Eo 'inet (addr:)?([0-9]*\.){3}[0-9]*' | grep -Eo '([0-9]*\.){3}[0-9]*' | grep -v '127.0.0.1')