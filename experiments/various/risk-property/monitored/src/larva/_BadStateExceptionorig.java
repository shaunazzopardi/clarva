package larva;
public class _BadStateExceptionorig extends Exception {
public String toString(){
String temp = "";
for (int i = 4; i < getStackTrace().length; i++) temp += "\r\n" + getStackTrace()[i];
return temp;
}
}