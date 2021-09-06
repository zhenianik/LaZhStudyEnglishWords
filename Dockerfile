FROM java:8
#за основу взята 8 версия джавы
ARG JAR_FILE=target/lazhstudyenglishwords-1.0.1.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]