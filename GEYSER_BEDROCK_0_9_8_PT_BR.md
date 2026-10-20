# Instalação experimental — Smart Copper Golems Extended 0.9.8 + Geyser-Fabric

Três componentes construídos automaticamente pelo GitHub Actions:

1. **Fabric 0.9.8** — instalado no servidor e clientes Java; sincroniza os movimentos e registra as transições de animação.
2. **GeyserBridge 0.9.8** — extensão instalada somente no Geyser-Fabric do servidor; registra a propriedade Bedrock `scg:reach` no Copper Golem.
3. **SmartCopperGolems_Bedrock_0.9.8.mcpack** — pack visual enviado aos clientes Bedrock. Preserva materiais, oxidação e animadores vanilla da definição Mojang.

## Instalação

Faça backup. Instale somente uma versão Smart Copper Golems Extended no servidor e cliente Java. Requer Geyser compatível com API 2.11.0.

- Servidor `mods/` → JAR Fabric 0.9.8.
- Servidor `config/Geyser-Fabric/extensions/` → JAR da extensão GeyserBridge. Não colocar esse JAR em mods.
- Servidor `config/Geyser-Fabric/packs/` → arquivo .mcpack Bedrock.
- Cliente Java `mods/` → o mesmo Fabric 0.9.8.
- Cliente Bedrock → aceitar o Resource Pack ao entrar no servidor; nenhum Behavior Pack ou Fabric local.

Reinicie o servidor inteiro; confira no log se a extensão Geyser carregou.

## Como funciona

O mod grava no arquivo local `config/smart-copper-golem/geyser-stretch-states.csv` o UUID do golem, sua fase (crescer/manter/encolher), o alcance e o horário da transição. A extensão Geyser no mesmo servidor verifica o arquivo a cada 100 milissegundos e publica um valor de 0 a 1 no Copper Golem correspondente para cada jogador Bedrock. O pack usa Molang `query.property('scg:reach')` para animar o corpo e as pernas, sem Behavior Pack. Os ciclos de crescer/encolher levam aproximadamente 2,55 segundos cada.

**Requisito:** mod Fabric e Geyser-Fabric rodarem no mesmo servidor, compartilhando o mesmo disco/pasta de configuração. Se estiverem em máquinas separadas, essa ponte local precisará ser substituída.

## Artifacts no GitHub Actions

- Fabric prototype validation → smart-copper-golems-extended-fabric-prototype
- Build Geyser Copper Golem extension → SmartCopperGolems_GeyserBridge_0.9.8
- Build Bedrock visuals → SmartCopperGolems_Bedrock_0.9.8

## Validação obrigatória antes de uso no mundo principal

Confirmar inicialização da extensão, instalação do pack no Bedrock, sincronização em golems a 2–5 blocos de altura, texturas de todas as oxidações, braços vanilla e jogador entrando no meio da animação. Testar vários golems simultâneos e reinicialização do servidor. A compilação CI não comprova que a integração visual funciona de ponta a ponta. A API de entidades Geyser ainda é experimental.

Este projeto é um PR experimental e **não deve ser mesclado ou publicado como release estável sem teste real**.
