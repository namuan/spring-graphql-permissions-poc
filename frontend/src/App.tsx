import { useEffect, useMemo, useState } from 'react'
import { identity, keycloak } from './auth/keycloak'
import { graphqlRequest, type GraphQLResult } from './graphql/client'
import getMyOrders from './graphql/queries/GetMyOrders.graphql?raw'
import getOrders from './graphql/queries/GetOrders.graphql?raw'
import getOrder from './graphql/queries/GetOrder.graphql?raw'
import createOrder from './graphql/mutations/CreateOrder.graphql?raw'
import updateOrder from './graphql/mutations/UpdateOrder.graphql?raw'
import cancelOrder from './graphql/mutations/CancelOrder.graphql?raw'
import deleteOrder from './graphql/mutations/DeleteOrder.graphql?raw'

const playgroundScenarios = [
  {
    label: 'Valid GetOrder',
    operationName: 'GetOrder',
    query: getOrder,
    variables: '{ "id": "1" }'
  },
  {
    label: 'Anonymous operation',
    operationName: '',
    query: '{ myOrders { id } }',
    variables: '{}'
  },
  {
    label: 'Unknown operation',
    operationName: 'UnknownOperation',
    query: 'query UnknownOperation { myOrders { id } }',
    variables: '{}'
  },
  {
    label: 'GetOrder using Query.orders',
    operationName: 'GetOrder',
    query: 'query GetOrder { orders { id amount } }',
    variables: '{}'
  },
  {
    label: 'Customer requests internalNotes',
    operationName: 'GetOrder',
    query: 'query GetOrder($id: ID!) { order(id: $id) { id internalNotes } }',
    variables: '{ "id": "1" }'
  }
]

type Order = {
  id: string
  description: string
  amount: string | number
  status: string
  customer: { id: string; username: string }
  internalNotes?: string | null
}

function responseData(result: GraphQLResult | null): any {
  if (!result || typeof result.body !== 'object' || result.body === null) return null
  return (result.body as { data?: unknown }).data
}

function responseErrors(result: GraphQLResult | null): string[] {
  if (!result || typeof result.body !== 'object' || result.body === null) return []
  const errors = (result.body as { errors?: Array<{ message?: string }> }).errors ?? []
  return errors.map((error) => error.message ?? 'Unknown GraphQL error')
}

export default function App() {
  const user = useMemo(() => identity(), [])
  const staff = user.roles.includes('SUPPORT') || user.roles.includes('ADMIN')
  const [view, setView] = useState<'orders' | 'management' | 'playground'>('orders')
  const [orders, setOrders] = useState<Order[]>([])
  const [result, setResult] = useState<GraphQLResult | null>(null)
  const [loading, setLoading] = useState(false)
  const [description, setDescription] = useState('')
  const [amount, setAmount] = useState('10.00')
  const [scenario, setScenario] = useState(playgroundScenarios[0])
  const [editor, setEditor] = useState(scenario.query)
  const [operationName, setOperationName] = useState(scenario.operationName)
  const [variables, setVariables] = useState(scenario.variables)

  const loadOrders = async () => {
    setLoading(true)
    const query = staff ? getOrders : getMyOrders
    const response = await graphqlRequest(query, {}, staff ? 'GetOrders' : 'GetMyOrders')
    const data = responseData(response)
    setOrders(data?.[staff ? 'orders' : 'myOrders'] ?? [])
    setResult(response)
    setLoading(false)
  }

  useEffect(() => {
    void loadOrders()
  }, [staff])

  const create = async () => {
    const response = await graphqlRequest(
      createOrder,
      { input: { description, amount } },
      'CreateOrder'
    )
    setResult(response)
    if (responseData(response)) await loadOrders()
  }

  const manage = async (order: Order, action: 'update' | 'cancel' | 'delete') => {
    let response: GraphQLResult
    if (action === 'update') {
      const nextDescription = window.prompt('Description', order.description)
      if (nextDescription === null) return
      const nextAmount = window.prompt('Amount', String(order.amount))
      if (nextAmount === null) return
      response = await graphqlRequest(updateOrder, { id: order.id, input: { description: nextDescription, amount: nextAmount } }, 'UpdateOrder')
    } else if (action === 'cancel') {
      response = await graphqlRequest(cancelOrder, { id: order.id }, 'CancelOrder')
    } else {
      response = await graphqlRequest(deleteOrder, { id: order.id }, 'DeleteOrder')
    }
    setResult(response)
    await loadOrders()
  }

  const executePlayground = async () => {
    let parsedVariables: Record<string, unknown> = {}
    try {
      parsedVariables = JSON.parse(variables || '{}')
    } catch {
      setResult({ status: 0, body: { errors: [{ message: 'Variables must be valid JSON' }] } })
      return
    }
    setResult(await graphqlRequest(editor, parsedVariables, operationName || undefined))
  }

  const selectScenario = (selected: typeof playgroundScenarios[number]) => {
    setScenario(selected)
    setEditor(selected.query)
    setOperationName(selected.operationName)
    setVariables(selected.variables)
  }

  return (
    <div className="shell">
      <header className="topbar">
        <div>
          <h1>GraphQL Security POC</h1>
          <p>Authentication, operations, capabilities, resources, and fields</p>
        </div>
        <div className="identity">
          <strong>{user.username}</strong>
          <span>Tenant {user.tenant}</span>
          <span>{user.roles.join(', ') || 'No mapped roles'}</span>
          <button onClick={() => keycloak.logout()}>Log out</button>
        </div>
      </header>

      <nav className="tabs">
        <button className={view === 'orders' ? 'active' : ''} onClick={() => setView('orders')}>My orders</button>
        {staff && <button className={view === 'management' ? 'active' : ''} onClick={() => setView('management')}>Tenant management</button>}
        <button className={view === 'playground' ? 'active' : ''} onClick={() => setView('playground')}>Security playground</button>
      </nav>

      {view === 'orders' && <OrdersView orders={orders} loading={loading} onCreate={create} description={description} setDescription={setDescription} amount={amount} setAmount={setAmount} />}
      {view === 'management' && <ManagementView orders={orders} loading={loading} roles={user.roles} onAction={manage} />}
      {view === 'playground' && (
        <section className="panel playground">
          <h2>Security playground</h2>
          <p className="muted">The backend still authorizes every request. The editor is for demonstration, not a security boundary.</p>
          <div className="scenarios">
            {playgroundScenarios.map((item) => <button key={item.label} onClick={() => selectScenario(item)}>{item.label}</button>)}
          </div>
          <label>Operation name<input value={operationName} onChange={(event) => setOperationName(event.target.value)} placeholder="GetOrder" /></label>
          <label>Variables<textarea value={variables} onChange={(event) => setVariables(event.target.value)} rows={3} /></label>
          <label>GraphQL<textarea className="editor" value={editor} onChange={(event) => setEditor(event.target.value)} rows={12} /></label>
          <button className="primary" onClick={() => void executePlayground()}>Execute</button>
          <ResponsePanel result={result} />
        </section>
      )}
    </div>
  )
}

function OrdersView(props: {
  orders: Order[]
  loading: boolean
  onCreate?: () => void
  description?: string
  setDescription?: (value: string) => void
  amount?: string
  setAmount?: (value: string) => void
}) {
  return (
    <section className="panel">
      <div className="panel-heading"><h2>Orders</h2>{props.loading && <span>Loading…</span>}</div>
      {props.onCreate && (
        <div className="create-form">
          <input value={props.description} onChange={(event) => props.setDescription?.(event.target.value)} placeholder="Description" />
          <input value={props.amount} onChange={(event) => props.setAmount?.(event.target.value)} placeholder="Amount" />
          <button className="primary" onClick={props.onCreate}>Create order</button>
        </div>
      )}
      <div className="order-grid">
        {props.orders.map((order) => <article className="order" key={order.id}>
          <div className="order-top"><strong>#{order.id}</strong><span className="status">{order.status}</span></div>
          <h3>{order.description}</h3>
          <p>Amount: {order.amount}</p><p>Customer: {order.customer.username}</p>
          {order.internalNotes !== undefined && <p className="internal">Internal: {order.internalNotes ?? '—'}</p>}
        </article>)}
      </div>
      {!props.loading && props.orders.length === 0 && <p className="muted">No orders visible.</p>}
    </section>
  )
}

function ManagementView(props: {
  orders: Order[]
  loading: boolean
  roles: string[]
  onAction: (order: Order, action: 'update' | 'cancel' | 'delete') => void
}) {
  const isAdmin = props.roles.includes('ADMIN')
  return <section className="panel">
    <div className="panel-heading"><h2>Tenant order management</h2>{props.loading && <span>Loading…</span>}</div>
    <p className="muted">All records are restricted to your tenant. UI actions are convenience only; the backend remains authoritative.</p>
    <div className="order-grid management-grid">
      {props.orders.map((order) => <article className="order" key={order.id}>
        <div className="order-top"><strong>#{order.id}</strong><span className="status">{order.status}</span></div>
        <h3>{order.description}</h3>
        <p>Amount: {order.amount}</p><p>Customer: {order.customer.username}</p>
        {order.internalNotes !== undefined && <p className="internal">Internal: {order.internalNotes ?? '—'}</p>}
        <div className="actions">
          <button onClick={() => props.onAction(order, 'update')}>Edit</button>
          {order.status === 'OPEN' && <button onClick={() => props.onAction(order, 'cancel')}>Cancel</button>}
          {isAdmin && <button onClick={() => props.onAction(order, 'delete')}>Delete</button>}
        </div>
      </article>)}
    </div>
    {!props.loading && props.orders.length === 0 && <p className="muted">No orders visible.</p>}
  </section>
}

function ResponsePanel({ result }: { result: GraphQLResult | null }) {
  if (!result) return null
  const errors = responseErrors(result)
  return <div className="response"><strong>HTTP {result.status}</strong>{errors.length > 0 && <ul>{errors.map((error, index) => <li key={index}>{error}</li>)}</ul>}<pre>{JSON.stringify(result.body, null, 2)}</pre></div>
}
