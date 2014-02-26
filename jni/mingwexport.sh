pexports smartbus_net_cli.dll | sed "s/^_//" > smartbus_net_cli.def
dlltool -d smartbus_net_cli.def -l libsmartbus_net_cli.a