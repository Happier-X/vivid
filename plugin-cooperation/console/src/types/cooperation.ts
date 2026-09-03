export interface CooperationSpec {
  company: string;
  contact: string;
  phone: string;
  type: string;
  typeLabel: string;
  message: string;
  website?: string;
  sourceUrl?: string;
  userAgent?: string;
  timestamp?: string;
  ip?: string;
  handled?: boolean;
}

export interface Cooperation {
  apiVersion: string;
  kind: string;
  metadata: {
    name: string;
    creationTimestamp?: string;
    version?: number;
  };
  spec: CooperationSpec;
}

export interface ListResult<T> {
  page: number;
  size: number;
  total: number;
  items: T[];
  first?: boolean;
  last?: boolean;
  hasNext?: boolean;
  hasPrevious?: boolean;
  totalPages?: number;
}

export type HandledFilter = "all" | "true" | "false";
export type TypeFilter =
  | ""
  | "all"
  | "institution"
  | "community"
  | "home_government"
  | "channel_oem";

export const TYPE_OPTIONS: Array<{ label: string; value: string }> = [
  { label: "全部类型", value: "all" },
  { label: "养老机构合作", value: "institution" },
  { label: "社区智慧养老", value: "community" },
  { label: "居家与政务合作", value: "home_government" },
  { label: "渠道代理 & OEM/ODM", value: "channel_oem" },
];

export const HANDLED_OPTIONS: Array<{ label: string; value: HandledFilter }> = [
  { label: "全部状态", value: "all" },
  { label: "未处理", value: "false" },
  { label: "已处理", value: "true" },
];

export const TYPE_LABEL_MAP: Record<string, string> = {
  institution: "养老机构合作",
  community: "社区智慧养老",
  home_government: "居家与政务合作",
  channel_oem: "渠道代理 & OEM/ODM",
};
