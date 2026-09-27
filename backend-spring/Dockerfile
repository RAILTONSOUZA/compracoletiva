# Etapa 1: Build da aplicação Java com Maven
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copia os arquivos do Maven e pom.xml para aproveitar o cache
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw dependency:resolve

# Copia o código fonte e as planilhas do catálogo
COPY src ./src
COPY PRODUTO.xlsx CLIENTE.xlsx ./

# Compila o JAR do Spring Boot
RUN ./mvnw package -DskipTests

# Etapa 2: Imagem final leve para execução
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Copia o JAR compilado e as planilhas
COPY --from=build /app/target/compracoletiva-*.jar app.jar
COPY --from=build /app/PRODUTO.xlsx /app/CLIENTE.xlsx ./

# Expõe a porta padrão
EXPOSE 8080

# Inicia a aplicação Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]
