# Resource Pack Bedrock para Geyser-Fabric

- Esta pasta é um **Resource Pack visual**, não um Behavior Pack. Não contém scripts/cheats.
- `entity/copper_golem.entity.json` é adaptado da amostra oficial da Mojang (mantidos os estados de oxidação, controladores de braços/cabeça/baú, materiais e texturas vanilla).
- `animations/smart_copper_golem.animation.json` avalia `query.property('scg:reach')` de 0 a 1 e interpola os keyframes da animação Blockbench no corpo e nas pernas.
- Não instale arquivos JSON isolados na pasta `resourcepacks`. Gere o `.mcpack` usando `python tools/build_bedrock_pack.py`, ou baixe o artifact do GitHub Actions.
- Para que a animação ocorra, o servidor Geyser deve registrar e atualizar a propriedade `scg:reach` via extensão; sem a ponte, não há animação adicional.
- **Importante:** a aparência Bedrock precisa de teste in-game, inclusive renderização e compatibilidade com EMF/oxidação.
