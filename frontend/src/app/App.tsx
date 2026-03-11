import { Layout, Menu, Typography } from 'antd';

const { Header, Content } = Layout;

export default function App() {
  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Header style={{ display: 'flex', alignItems: 'center' }}>
        <Typography.Title level={4} style={{ color: '#fff', margin: 0, marginRight: 24 }}>
          StableFlow
        </Typography.Title>
        <Menu
          theme="dark"
          mode="horizontal"
          selectable={false}
          items={[
            { key: 'dashboard', label: 'Dashboard' },
            { key: 'invoices', label: 'Invoices' },
            { key: 'exceptions', label: 'Exceptions' },
            { key: 'agent', label: 'Agent' }
          ]}
        />
      </Header>
      <Content style={{ padding: 24 }}>
        <Typography.Title level={2}>StableFlow Workspace</Typography.Title>
        <Typography.Paragraph>
          Backend and frontend skeleton initialized. Next step is wiring the invoice and payment flow.
        </Typography.Paragraph>
      </Content>
    </Layout>
  );
}
