# Smart Copper Golems 0.9.19 — detecção distante de fornalhas (hotfix)

Base: **0.9.18** `feature/furnace-config-bilingual-0.9.18`. Esta alteração foi isolada em `fix/furnace-detection-from-0.9.18-0.9.19`, sem modificar as outras profissões nem mesclar branches.

## Problema corrigido
O Fornalheiro era limitado a uma caixa de **12 blocos horizontais e 3 verticais ao redor da posição atual**, não importando o raio de trabalho. Se recebesse sua Name Tag mais longe, nunca descobria fornos a 30–60 blocos.

A nova IA busca diretamente as *block entities* de fornalhas normais, altos-fornos e defumadores em **chunks já carregados**, em vez de consultar cada bloco de uma região volumosa. O alcance é uma **área quadrada em X/Z** (até N blocos por eixo) com ±12 blocos verticais. Os fornos fora da área individual diurna/noturna configurada são ignorados.

## Comandos em português e inglês

| Ação | PT-BR | English |
|---|---|---|
| Ver alcance global | `/fornalheiro deteccao` | `/furnace_tender detection` |
| Alterar alcance global | `/fornalheiro deteccao 64` | `/furnace_tender detection 64` |
| Aumentar alcance global | `/fornalheiro deteccao 96` | `/furnace_tender detection 96` |
| Alterar alcance só de Carlos | `/fornalheiro Carlos deteccao 80` | `/furnace_tender Carlos detection 80` |
| Consultar alcance de Carlos | `/fornalheiro Carlos deteccao` | `/furnace_tender Carlos detection` |
| Restaurar Carlos para o global | `/fornalheiro Carlos deteccao reset` | `/furnace_tender Carlos detection reset` |
| Restaurar global para 64 | `/fornalheiro deteccao reset` | `/furnace_tender detection reset` |

Também disponíveis via `/scg fornalheiro deteccao ...` e `/scg furnace detection ...`. Os comandos de **quantidade de combustível** da 0.9.18, como `/fornalheiro Carlos combustivel 32`, continuam iguais.

**Padrão:** 64 blocos horizontalmente (antes: 12). Valores permitidos: múltiplos de 8, de 16 a 128. Os ajustes sobrevivem à reinicialização do mundo em `smart-copper-golem-furnaces.properties`, usando o UUID de cada golem para personalização.

## Desempenho e segurança
- São inspecionadas somente as *block entities* que já existem em chunks carregados. O mod **não carrega chunks**, não cria tickets e não simula fornalhas à distância.
- Busca atualizada aproximadamente a cada 80 ticks (4 segundos) quando o Fornalheiro está livre; antiga verificação local a cada 20 ticks foi substituída para compensar a área maior.
- Até 96 candidatos, ordenados por proximidade, por tentativa, para evitar sobrecarga em fábricas enormes.
- A navegação aceita procurar fornos mais distantes, usando as etapas de caminhada de até 16 blocos já existentes. O prazo de entrega subiu para 1600 ticks, pois ir ao baú e depois ao forno pode levar mais de 24 segundos.
- O novo alcance **não aumenta** a área de trabalho configurada com `/copper_golem "Fornalheiro: Carlos" radius 30`; o menor limite vale. Também não ultrapassa área noturna menor.
- Nenhuma mudança nos filtros 0°/45°, combustível 8..64, comandos bilíngues, Agricultor, Coletor, Organizador, animações Geyser e categorias 42/1413.

## Teste prático
1. Em uma cópia do mundo, dê uma Name Tag `Fornalheiro: Carlos` a um golem a **40 blocos** da fornalha; deixe fornalha + baú de carvão em chunks carregados.
2. Execute `/fornalheiro Carlos deteccao 64` e, se necessário, aumente a área de trabalho individual com `/copper_golem "Fornalheiro: Carlos" radius 160`.
3. Espere uma busca (~4 segundos) e observe se encontra o alvo, vai ao baú e depois ao forno, abastecendo o slot combustível.
4. Faça testes a 16, 32, 48 e 64 blocos; teste `/furnace_tender Carlos detection 96` num mundo carregado maior.
5. Coloque um forno em chunk descarregado: **não** deve procurar esse forno até o chunk ficar carregado.
6. Bloqueie a entrada de um forno próximo; deve ignorar os claramente inacessíveis, sem quebrar ou duplicar carvão.
7. Teste `/fornalheiro Carlos combustivel 32`, 8 carvões, 1 bloco de carvão, reinicialização, turnos diurno/noturno, Coletor/Agricultor e conexão Bedrock.
8. Valide a contagem de carvão antes e depois, especialmente com dois Fornalheiros.

**Build compilada não garante teste em Minecraft**. Não faça merge até checar os cenários reais.
