# Smart Copper Golems Extended 0.9.22 — Baús e barris atribuídos a golems por nome

**Minecraft Java 26.2 / Fabric**, base `feature/shepherd-virtual-shears-0.9.21`. Nenhuma alteração ao port 26.3. PR experimental, sem merge.

## O caso de Roger na farm de cana
- Nomeie o golem com Name Tag **`Coletor: Roger`**, **`Roger`**, ou qualquer profissão com sufixo `: Roger` (para cana caída no chão, o **Coletor** é o indicado; Agricultor existente é exclusivo para trigo, batata, cenoura e beterraba).
- Coloque **um baú ou barril numa bigorna e renomeie o item `Roger`**, ANTES de colocá-lo no mundo.
- Coloque uma moldura nesse recipiente com **cana-de-açúcar**. Rotação 45° (1 passo) = filtro exato `minecraft:sugar_cane`; 0° = categoria inteira do item, conforme catálogo. O nome **não substitui a moldura**.
- Esse recipiente será **privativo do golem Roger**: outros golems ignoram o baú no recolhimento de itens, ao guardar, na compostagem, no fornecimento de carvão e na seleção dos baús. As verificações são refeitas antes de alterar o inventário, incluindo depósitos elevados por animação.
- Outros baús e barris **sem nome personalizado** continuam comuns, podendo ser usados por todos os golems conforme filtros de moldura.
- Quando Roger encontra tanto um baú pessoal quanto um compartilhado com o mesmo filtro, ele **prioriza o próprio**, mesmo que o compartilhado esteja mais perto. Caso o próprio esteja cheio ou fora de sua área de trabalho, usa outro válido, compartilhado, se houver.
- Para reutilizar um baú por todos os golems, quebre-o, renomeie na bigorna para **Baú** (sem nome personalizado de dono) e reinstale, ou coloque um baú novo sem nome. Não confunda a exibição do título de baú vanilla com nome personalizado: somente o customName armazenado pelo bloco o torna privado.
- **Jogadores, funis, água e redstone não são bloqueados por este recurso.** É isolamento da **IA dos golems**, não uma proteção contra roubo de players/hoppers.

## Baú duplo
- Se uma metade estiver renomeada `Roger` e a outra sem nome, **todo o inventário duplo** é exclusivo de Roger; outros golems não usam nenhuma das duas metades.
- Se as duas metades estiverem nomeadas, precisam possuir o **mesmo nome** (ignorando acentos e capitalização). Se uma for `Roger` e outra `Pedro`, nenhum golem pode usar esse baú duplo; segurança acima da conveniência.
- É recomendável testar primeiro com **baú simples ou barril**, antes de usar baú duplo.

## Nomes e ambiguidades
- São aceitos tanto os nomes **completos** quanto o apelido após dois-pontos: `Coletor: Roger` pode acessar tanto um recipiente com nome `Roger` quanto `Coletor: Roger`.
- Não há comparação parcial: `Rog` nunca dá acesso ao baú `Roger`.
- O recurso ignora maiúsculas e acentos: `João`, `JOÃO` e `JOAO` coincidem.
- **Nomes únicos são obrigatórios para a propriedade por nome.** Se dois golems carregados correspondem à identidade `Roger` (por exemplo, `Coletor: Roger` e `Pastor: Roger`), o mod recusa o acesso ao recipiente privado por ambos até eliminar a duplicação. Golems descarregados não são descobertos; por isso, garanta nomes únicos no mundo todo.
- O nome é relido do baú/barril ao longo da operação. Renomear o recipiente redireciona a propriedade; não depende de arquivo extra nem de reinicialização.
- O dono **ainda precisa estar dentro de sua área individual**, respeitar noite/turnos e molduras existentes. A propriedade não aumenta o raio.

## Teste em cópia do mundo (não migrar o save para 26.3)
1. Instale **somente** a versão 0.9.22 (26.2), substituindo o JAR 0.9.21 e mantendo Fabric 26.2 / Fabric API compatível.
2. Coloque `Coletor: Roger` na farm de cana e um barril `Roger` com moldura de cana a **45°**; garanta acesso pelo raio de trabalho.
3. Coloque outro baú comum na base com moldura de cana a **45°**. Deixe outros golems e Roger encontrá-los. Outros golems **nunca** devem depositar no baú Roger; Roger deve priorizar o próprio. Baú comum segue acessível para todos.
4. Renomeie um barril para `Pedro`; Roger não deve retirar nem depositar nada ali.
5. Teste guardar cana quando o baú pessoal estiver cheio: Roger pode guardar em baú compartilhado correspondente, sem sumir com itens.
6. Teste baú duplo uma metade `Roger` e outra normal, e conflito `Roger` + `Pedro`.
7. Teste nomes duplicados; nenhum dos dois proprietários ambíguos deve conseguir tocar no baú privado.
8. Teste um golem ao lado do baú enquanto outro jogador **renomeia/quebra** o recipiente. Nenhum item deve ser depositado no recipiente renomeado para outra pessoa.
9. Verifique todo o restante: Agricultor e farinha de osso, Fornalheiro e carvão, Pastor e lã, Coletor com filtro `/coletor Roger item minecraft:sugar_cane`, organizadores comuns e animações Bedrock/Geyser.

### Limites
- O sistema de nomes não pode impedir interações externas ao mod (funis e jogadores); não altera o bloco vanilla.
- Pela forma do nome, `Coletor: Roger` e `Roger` são compatíveis, mas não são dois **golems distintos** com permissão de usar o mesmo baú. Evite nomes repetidos.
- A execução do CI não é um teste de comportamento dentro de um servidor real. Teste o fluxo em cópia do mundo antes de promover a versão estável.
