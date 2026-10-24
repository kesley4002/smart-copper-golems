# Smart Copper Golems Extended 0.9.21 — Tesoura virtual na mão do Pastor

**Base:** versão 0.9.20 `feature/nether-wood-categories-shepherd-0.9.20`, aprovada em CI. Esta alteração ocorre numa branch própria, sem modificar ou mesclar a versão anterior.

## Visual solicitado
1. Nomeie o golem **`Pastor: João`**, **`Tosquiador: João`** ou **`Shepherd: John`**.
2. Enquanto está livre ou caminhando para tosquiar uma ovelha, o Pastor **exibe uma tesoura na mão principal**, visível no modelo normal de golem. É o item vanilla SHEARS com marcador interno de cosmético, sem durabilidade/consumo. Não é necessário produzir ou colocar tesouras num baú.
3. O Pastor se aproxima e executa tosquia vanilla com som e drops de lã na cor da ovelha.
4. **Imediatamente após tosquiar**, a tesoura some da mão, para que ele possa andar até a lã no chão. Continua escondida enquanto procura/recolhe o drop de lã.
5. O Pastor pega lã real no lugar da tesoura e caminha até o baú com moldura `wool` ou a cor exata, como na versão anterior.
6. Quando termina de guardar a lã, no próximo ciclo o instrumento visual **reaparece**. Se a lã for recolhida por outro jogador ou desaparecer, um timeout de aproximadamente 8 segundos devolve a aparência normal para impedir que o golem fique eternamente sem tesoura.
7. Nenhuma alteração nos itens reais, limites de carga e na lógica de baús, compostagem, Fornalheiro, Agricultor e Coletor.

## Tesoura virtual jamais vira item do jogador
- A tesoura cenográfica tem um **marcador privado de componente**. O mod só trata como virtual uma tesoura com esse marcador: tesouras legítimas do jogador NÃO são afetadas.
- Interagir com o golem enquanto a tesoura cosmética está visível **é interceptado no servidor**, a tesoura some momentaneamente e **nenhuma é transferida** ao inventário do jogador. O Pastor poderá voltar a mostrá-la quando retomar o trabalho.
- Uma salvaguarda em servidor remove de inventários dos jogadores somente o item especificamente marcado como virtual caso algum cliente Geyser/Bedrock ou outro mod contorne a interação.
- A tesoura virtual tem **chance zero de drop** no golem. Ao recarregar entidade, essa proteção é restaurada. Se alguma cópia marcada cair no mundo via outro mod/versão, é removida ao carregar a entidade.
- Se o Pastor for renomeado para outra profissão, a tesoura virtual é removida sem afetar tesouras reais.
- Para impedir acesso a essa tesoura, interações comuns com o golem com a tesoura à mostra são consumidas, por exemplo, clicar com mão vazia. É um bloqueio intencional, restrito à aparência virtual.

## Como validar no Minecraft (recomendado primeiro num backup)
1. Nomeie `Pastor: João`; ele deve aparecer com a tesoura na mão em instantes, parado ou andando.
2. Garanta ovelhas adultas com lã em um cercado, grama para regeneração e baú de lã com item frame a 0° (todas as cores) ou 45° (cor específica).
3. Observe todo o ciclo: **tesoura visível → aproximação → tosquia → tesoura some → pega lã → caminha até baú → deposita → tesoura reaparece**.
4. Clique com a mão vazia no Pastor com tesoura: a tesoura pode sumir por um instante, mas **jamais deve aparecer no seu inventário**.
5. Com seu inventário contendo uma tesoura real, interaja e confira que sua tesoura legítima não foi removida.
6. Renomeie o Pastor para `Coletor: João`: tesoura virtual deve desaparecer, o golem volta à profissão de Coletor sem pegar tesouras alheias.
7. Mate um Pastor num mundo de testes: não deve dropar tesoura virtual.
8. Mude para cliente Bedrock via Geyser e verifique se a tesoura é exibida como item vanilla e não é obtida por interação. Dependendo de como o cliente Bedrock renderiza mãos de Copper Golems, o modelo pode mostrar o item de outra maneira.
9. Reinicie o servidor e repita; confirme que os baús e as categorias 42/1413 continuam intactos.

**Status: experimental**, compilação e regressões automatizadas são necessárias e NÃO substituem os testes reais, sobretudo com Geyser.
