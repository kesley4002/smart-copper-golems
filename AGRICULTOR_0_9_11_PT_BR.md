# Agricultor — primeiro protótipo 0.9.11

Base: versão Coletor 0.9.10 validada em 31/31 testes pelo usuário. Não houve merge na versão principal.

## Name Tag
- Agricultor ou Agricultor: João (também aceita Farmer)
- O nome completo continua disponível nos comandos individuais /copper_golem "Agricultor: João" radius 30.
- Qualquer golem com nome comum continua sendo Organizador; Coletor continua coletando itens no chão.

## Funcionalidades neste protótipo
- Encontra trigo, cenoura, batata e beterraba no estágio maduro num raio local de busca de 10 blocos.
- Busca caminho até a plantação, colhe o loot da planta e replanta na mesma terra arada somente quando a própria colheita rende uma unidade da semente ou item de plantio. Nada é criado gratuitamente.
- Produtos e sementes excedentes viram itens no chão. Agricultor reutiliza a lógica existente do Coletor para buscá-los e levá-los aos recipientes com molduras compatíveis.
- Obedece ao raio individual e trabalho noturno. Não carrega chunks distantes nem altera terras não aradas.
- Mantém os filtros 0°/45°, 42 categorias e todas as animações e rotas da versão de armazenamento original.

## Ainda não implementado
- Plantio novo em terra arada vazia a partir de baú de sementes, independente de colheita.
- Alimentar composteira, recolher farinha de osso e reservar quantidades mínimas de sementes. Esses sistemas exigem uma camada de inventário e transação específica e não serão falsamente anunciados como prontos.
- Melancia, abóbora, cacau, cana, bambu e Nether wart (mecânicas de cultivo diferentes).
- Nenhuma alteração na implementação Bedrock nativa separada.

## Testes necessários antes de usar na base principal
1. Confirmar compilação Java 26.2 e catálogo completo.
2. Testar trigo, cenoura, batata, beterraba maduras, replantio e contagem rigorosa dos drops.
3. Testar planta imatura, bloco sem terra arada, falta de destino, baú cheio e raio limitado.
4. Testar coleta dos drops com moldura, ordenação, noite, reinício, múltiplos Agricultores e um Coletor.
5. Não mesclar nem chamar versão de agricultor completa até plantio autônomo e compostagem.
