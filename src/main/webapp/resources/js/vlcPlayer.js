        /**
        play a file using vlc IE only!
        from https://wiki.videolan.org/ActiveX/
        */
        function play(uid,url) {
            var htmlstr = '<embed type="application/x-vlc-plugin" pluginspage="http://www.videolan.org"/>' + '\n'
                        + '<OBJECT' + '\n'
                        + ' classid="clsid:9BE31822-FDAD-461B-AD51-BE1D1C159921"' + '\n'
                        + ' codebase="http://downloads.videolan.org/pub/videolan/vlc/latest/win32/axvlc.cab"' + '\n'
                        + ' width="0" height="0"' + '\n'
                        + ' events="True"' + '\n'
                        + '>' + '\n'
                        + '    <param name="Src" value="' + url + '" />' + '\n'
                        + '    <param name="ShowDisplay" value="False" />' + '\n'
                        + '    <param name="AutoPlay" value="True" />' + '\n'
                        + '</OBJECT>' + ' \n'
            var palyerdiv = document.getElementById(uid+"vlcplayerholder");
            palyerdiv.innerHTML = htmlstr;
        }