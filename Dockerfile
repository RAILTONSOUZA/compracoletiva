# Dockerfile na raiz do repositório
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copia os arquivos do Maven e pom.xml
COPY backend-spring/pom.xml backend-spring/mvnw ./
COPY backend-spring/.mvn ./.mvn

# Dá permissão de execução ao mvnw
RUN chmod +x ./mvnw

# Resolve dependências
RUN ./mvnw dependency:resolve

# Copia o código fonte e as planilhas do catálogo
COPY backend-spring/src ./src
COPY backend-spring/PRODUTO.xlsx backend-spring/CLIENTE.xlsx ./

# Compila o JAR do Spring Boot
RUN ./mvnw package -DskipTests

# Etapa final: execução
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /app/target/compracoletiva-*.jar app.jar
COPY --from=build /app/PRODUTO.xlsx /app/CLIENTE.xlsx ./

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
