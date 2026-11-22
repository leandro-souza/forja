# Forja

Sistema pessoal de treino, nutrição e evolução. Gera protocolos de musculação em divisão ABCD com um dia dedicado a um grupo muscular, monta o cardápio diário com alimentos comuns no Nordeste e usa os registros de cada protocolo para gerar o seguinte.

## O que o sistema faz

- **Avaliação inicial (anamnese):** dados corporais, rotina, dores, restrições alimentares e triagem de prontidão para o exercício.
- **Protocolos periodizados:** readaptação, base, três semanas de choque em pirâmide crescente e uma de recuperação. Exercícios em máquinas e polias, filtrados pela carga sobre a coluna.
- **Treino guiado:** foto da posição inicial e final de cada exercício, instruções de execução, link para vídeo, carga sugerida por série, cronômetro de execução com marcação de cadência e contagem regressiva de descanso com som e vibração.
- **Nutrição:** metas de calorias e macronutrientes (Mifflin-St Jeor com superávit de 10%), cardápio diário sem lactose, suplementos e lista de compras semanal.
- **Evolução:** peso, medidas, força estimada por exercício e análise do protocolo.
- **Protocolos adaptativos:** o próximo protocolo mantém os exercícios que mais geraram ganho, troca os estagnados, dá volume extra aos dois grupos que mais evoluíram, substitui exercícios associados a dor na coluna e ajusta as calorias pela variação de peso.
- **Aplicativo instalável (PWA)** e cópia de segurança em JSON.

## Arquitetura

```
frontend/   React 18 + TypeScript + Vite + Tailwind CSS 4 (PWA)
backend/    Java 21 + Spring Boot 3 (Web, Data JPA) + PostgreSQL
  core/     regras de negócio em Java puro, sem dependência de framework
            ExerciseCatalog, ProtocolEngine, Analysis, Nutrition
  domain/   entidades JPA e repositórios
  service/  TrainingService: orquestra protocolo, sessões e análise
  web/      controllers REST (/api)
  config/   filtro de autenticação por token e cache de arquivos estáticos
Dockerfile  build em três estágios; a API serve a interface no mesmo domínio
```

O plano de cada protocolo é gravado como JSON na tabela `protocols`; as séries executadas ficam em `set_logs`. As regras de geração e análise estão isoladas em `core/`, o que permite testá-las sem banco nem servidor.

## Executar localmente

```bash
cd frontend && npm install && npm run build
mkdir -p ../backend/src/main/resources/static && cp -r dist/* ../backend/src/main/resources/static/
cd ../backend && mvn spring-boot:run        # http://localhost:8080 (banco H2 em arquivo)
```

Ou com Docker: `docker build -t forja . && docker run -p 8080:8080 forja`.

## Variáveis de ambiente

| Variável | Uso |
| --- | --- |
| `JDBC_DATABASE_URL` | URL JDBC do PostgreSQL, por exemplo `jdbc:postgresql://host:5432/forja` |
| `DB_USER`, `DB_PASSWORD` | Credenciais do banco |
| `PORT` | Porta HTTP (padrão 8080) |
| `ALLOW_SIGNUP` | `true` libera novos cadastros. Por padrão só o primeiro cadastro é aceito |
| `FORJA_SEED` | JSON opcional com `profile` e `measurements` aplicados ao primeiro cadastro |

## Testes

`cd backend && mvn test` executa os testes do motor de protocolos e do cardápio.

## Créditos

Fotos dos exercícios: [free-exercise-db](https://github.com/yuhonas/free-exercise-db), domínio público (Unlicense).

As orientações de treino, dieta e suplementação são informativas e não substituem acompanhamento médico, nutricional ou de educação física presencial.
