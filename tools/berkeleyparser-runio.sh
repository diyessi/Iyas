#!/bin/bash

# ./bin/berkeleyparser-run.sh <FILE>

test=$1;

#dotest="java -mx3000m -Dfile.encoding=UTF-8 -jar /Users/oliunya/TextPro1.5.2_MacOSX/ParseBer/italian_parser/BerkeleyParser-Italian/berkeleyParser.jar -accurate -useGoldPOS -maxLength 250 -gr "

#dotest="java -mx3000m -Dfile.encoding=UTF-8 -jar /home/antonio/workspace-java/Iyas/tools/TextPro1.5.2_Linux64bit/ParseBer/italian_parser/BerkeleyParser-Italian/berkeleyParser.jar -accurate -useGoldPOS -maxLength 250 -gr "

dotest="java -mx3000m -Dfile.encoding=UTF-8 -jar tools/TextPro1.5.2_Linux64bit/ParseBer/italian_parser/BerkeleyParser-Italian/berkeleyParser.jar -accurate -useGoldPOS -maxLength 250 -gr "

part="fulltrain";

#$dotest /home/antonio/workspace-java/Iyas/tools/TextPro1.5.2_Linux64bit/ParseBer/italian_parser/BerkeleyParser-Italian/tutall-$part | /home/antonio/workspace-java/Iyas/tools/TextPro1.5.2_Linux64bit/ParseBer/italian_parser/BerkeleyParser-Italian/bin/it2eng_prs.pl

#$dotest /home/antonio/workspace-java/Iyas/tools/TextPro1.5.2_Linux64bit/ParseBer/italian_parser/BerkeleyParser-Italian/tutall-$part

$dotest tools/TextPro1.5.2_Linux64bit/ParseBer/italian_parser/BerkeleyParser-Italian/tutall-$part


#$dotest /Users/oliunya/TextPro1.5.2_MacOSX/ParseBer/italian_parser/BerkeleyParser-Italian/tutall-$part |/Users/oliunya/TextPro1.5.2_MacOSX/ParseBer/italian_parser/BerkeleyParser-Italian/bin/it2eng_prs.pl
