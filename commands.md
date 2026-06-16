# Commands

## Argument<T, K>

- Key
- ArgumentType(K)

- suggestions
- children
- executor

- onMissing
- onMissingChild

- isSyntaxValid
- transformValue: (K) -> TransformResult<\T>
- isValid

- isOptional

### Key

Type: String

Used to identify node key to get the value

### ArgumentType

Type: ArgumentType\<K>

Used to get the type of the argument. Inherently bound to what
Brigardier/CommandAPI does. Only relevant for the compiler.

### Suggestion

Type: Context.() -> List\<Suggestion> Context = (sender: CommandSender, args:
Map\<String, Any>) Suggestion = (value: String, tooltip: String)

Used to get the suggestions for the argument.

### Children

Type: List\<Argument> Used to get the children of the argument.

### Executor

Type: CommandExecutor (Context.() -> Unit)

Used to execute the command. Is executed after the rest of validation occured.

### onMissing

Type: CommandExecutor (Context.() -> Unit)

Is executed by the previous node if it didn't have a executor, and this node
wasn't provided.

### onMissingChild

Type: CommandExecutor (Context.() -> Unit)

Is executed if no child has a onMissing, this node has no onExecute, and further
nodes were not provided.

### isSyntaxValid

Type: SyntaxContext\<K>.() -> SyntaxResult

Used to validate the syntax of the argument while it is typed.

### transformValue

Type: Context.(K) -> TransformResult<\T>

Used to transform the value of the argument. Is called before IsValid, it being
responsible for handling error states of TransformResult.

### isValid

Type: Context.(TransformResult) -> Boolean // with side effects

Used to validate the argument value and provide error messages.

### isOptional

Type: Boolean

If this isOptional, a copy of every other node at this level is made which
features this node before it.
