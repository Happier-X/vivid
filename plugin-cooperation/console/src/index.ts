import { definePlugin } from "@halo-dev/console-shared";
import CooperationList from "./views/CooperationList.vue";
import { IconPlug } from "@halo-dev/components";
import { markRaw } from "vue";

export default definePlugin({
  routes: [
    {
      parentName: "Root",
      route: {
        path: "/cooperations",
        name: "Cooperations",
        component: CooperationList,
        meta: {
          title: "合作咨询",
          searchable: true,
          menu: {
            name: "合作咨询",
            group: "业务管理",
            icon: markRaw(IconPlug),
            priority: 30,
          },
        },
      },
    },
  ],
  extensionPoints: {},
});
