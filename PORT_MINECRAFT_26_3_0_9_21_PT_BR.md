# Smart Copper Golems Extended — Port experimental da 0.9.21 para Minecraft 26.3

**Origem:** `feature/shepherd-virtual-shears-0.9.21` (Minecraft 26.2, SHA `0d7247640e1a46470f8fba2b882028fa4eb955f1`).

**Destino:** `port/fabric-26.3-shepherd-0.9.21` (Minecraft **Java 26.3**, Fabric).

Este é um *port*, não um merge. Preserve todos os JARs 26.2 e instale o JAR 26.3 **somente depois de atualizar o servidor** e guardar cópia do mundo.

## Atualização de dependências

| Ferramenta | 26.2 | 26.3 |
|---|---|---|
| Minecraft Java | 26.2 | **26.3** |
| Java | 25 | **25** |
| Fabric Loader | 0.19.3 | **0.19.5** |
| Fabric Loom | 1.17-SNAPSHOT | **1.18.4** |
| Gradle | 9.5.1 | **9.7.1** |
| Fabric API | 0.152.1+26.2 | **0.165.0+26.3** |
| Mod | Extended 0.9.21 | **Extended 0.9.21+mc26.3-port.alpha.1** |

Fonte das mudanças de API: [Fabric para 26.3](https://www.fabricmc.net/2026/09/15/263.html), [Fabric API](https://github.com/FabricMC/fabric-api/releases), [guia de port 26.2→26.3](https://docs.neoforged.net/primer/docs/26.3/).

## Compatibilidade de código
- As chamadas antigas `mob.swing(InteractionHand.MAIN_HAND)` não compilam na 26.3. Migradas para `mob.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT)`, mantendo a animação vanilla.
- Metadados `fabric.mod.json` especificam Minecraft `~26.3` e Loader `>=0.19.5` (não anuncia compatibilidade incorreta com 26.2).
- As categorias 42/1.413, as traduções e os arquivos de configuração persistidos continuam **os mesmos**. **Itens introduzidos na 26.3 não estão necessariamente classificados pelo catálogo estático da 26.2**; os itens de poplar, cushions, straw beds e slabs/stairs novas podem precisar de etapa específica de ampliação da lista.
- Não foram alteradas as regras de proteção de baús, fábrica de combustível, reservas agrícolas, comandos ou filtros já configurados.

## Funcionalidades a validar no jogo
1. Antes de atualizar: faça backup completo da pasta do mundo e da pasta `mods`. Preferencialmente teste **uma cópia** em um segundo servidor 26.3.
2. Instale Minecraft Server 26.3 + Fabric Loader 0.19.5 + Fabric API compatível com 26.3 (0.165.0+26.3) + este JAR; **remova o JAR 26.2**, não deixe dois Smart Copper Golems.
3. Confirme inicialização sem erros de mixin/NoSuchMethodError e que comandos `/scg`, `/coletor`, `/fornalheiro`, `/agricultor` respondem.
4. Organizador: baús simples/duplos, barris e shulkers com item frames 0°/45°, classificação, ordenação, áreas e horário noturno.
5. Coletor: `/coletor Pedro item ovo`, teste recolhimento exclusivo de ovos e persistência após restart.
6. Agricultor: plantio/colheita, rotas até baús, compostagem com reserva, bone meal e coleta restrita a cultivo.
7. Fornalheiro: detecção a até 64 blocos em chunks carregados, fuel 8–64, armazenamento sem consumir insumos.
8. Pastor: 16 cores de lã, uso de tesouras virtuais, não entregar tesouras ao jogador, reinicialização, comportamento visual e segurança de drops.
9. Faça login de cliente Java e de cliente **Bedrock via Geyser/Floodgate** e confira inventários, molduras, animação de baús elevados e visualização da tesoura.
10. Verifique novos itens do bioma dappled forest / madeira poplar e anote quais ainda precisam ser categorizados.

## Observações
- A atualização do mundo é normalmente **unidirecional**: não faça downgrade do mesmo arquivo de mundo da 26.3 de volta para 26.2; restaure o backup, se necessário.
- Dependências de terceiros (Geyser/Floodgate/Playit etc.) precisam estar compatíveis com o servidor 26.3 de forma independente. Este port não atualiza plugins/mods externos.
- A aprovação de Gradle/CI demonstra **compilação e testes automatizados**, não equivale a testar o mod num servidor com jogadores.
- **Não mesclar** o PR antes de validar as rotinas principais dentro do Minecraft.
