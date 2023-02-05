# scheduling

This is a software to schedule up to 30 employees in the so called "Arbeitgebermodell" for disabled people. As an input the program takes a spreadsheet which can be derivied from the ```scheduling.ots``` file in the root directory of this repository.

## Prerequisites

* The software runs on at least Java 11 or higher. The spreadsheet can be provided as an argument on console. If the program is started without any arguments on console or with one than more argument, the program starts with a user interface. The user then can select the input file via file chooser. Afterwards the user can hit ```ENTER``` or use the start button the run the program. The program terminates after some time and writes the result to an output file located in the same directory as the input file. In the user interface the customer can stop the program via the specific button. Then the output file is written immediately. However, it is recommended tolet the program run until the end to get optimal results.

* To generate the spreadsheet and to show the result LibreOffice is nedded.

* It is recommended to activate macros to use all features of the spreadsheet. Otherwise the 'Freizeit' tables will not work properly.

## Features
Right now there are the following features:

* By setting any background color for an employee an a specific day, you can indicate that the employee is not available on that day.
* by setting 'x' for an employee on a specific day, you indicate that this employee has to work on that day. There is always exact one employee per day.
* Below the schedule, you can determine whether a day as a free day ('F' - e.g. Saturday, Sunday, public holiday). The spreadsheet comüputes free days on Saturday and Sunday automatically.
* One futher line below every shift a set as a single shift ('E'). If something else then 'E' is put in there, it means that the employee on that day and the following day has to be the same.
* Column 'AG' indicates whether the employee is charged arccording to the "Arbeitgebermodell" or not (e.g. when the employee is charged according to an external nursing service). For the scheduling this is not relevant. However, when in this column you put 'FALSE', then the employee will not appear in the other tables, which are relevant for diffrent funding agencies.
* Column 'AH': The amount of times the employee is working in the specific month. The sum of this column must match the length of the month. Otherwise you will get a red sum field. If everthing is fine, this sum field will become green. If this does not match something random will happen. A non positive value is interpreted as zero.
* Column 'AI': The amount of free days the employee is intended to work. The value of this column must always be less or equal than 'AH'. Furthermore, the sum of this column must match the amount of free days. If this is the case, the sum field will become green, otherwise it is red and something random will happen. A non positive value is interpreted as zero.
* Column 'AJ': This is the wished length of each block shift for each employee. The length of a block shift is the number of direct following shifts. If no value, a negative value or zero is set, it means that it does not matter
* Column 'AK': This is the maximum length of a block shift. The value must be greater or equal to the one in column 'AJ'. Otherwise the value from column 'AK' is taken for the value in 'AJ'. If no value, a negative value or zero is set, it means that it does not matter.

## Known issues

When yo copy the resilt of the program from the output file to the input file, you will lose the borders of the schedule. It is recommended to just copy the text and not the whole content. This is an issue of the library that deals with spreadsheet.

Contributions are welcome!