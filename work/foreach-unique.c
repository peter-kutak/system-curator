#include <stdio.h>
#include <string.h>
#include <systemd/sd-journal.h>

int main(int argc, char *argv[]) {
  sd_journal *j;
  const void *d;
  size_t l;
  int r;

  r = sd_journal_open(&j, SD_JOURNAL_LOCAL_ONLY);
  if (r < 0) {
    fprintf(stderr, "Failed to open journal: %s\n", strerror(-r));
    return 1;
  }
  r = sd_journal_query_unique(j, "_SYSTEMD_UNIT");
  if (r < 0) {
    fprintf(stderr, "Failed to query journal: %s\n", strerror(-r));
    return 1;
  }
//  SD_JOURNAL_FOREACH_UNIQUE(j, d, l)
//    printf("%d %s\n", (int) l, (const char*) d);
  sd_journal_close(j);

  printf("sizeof sd_id128_t %d\n", sizeof(sd_id128_t));
  printf("sizeof byte[16]   %d\n", sizeof(uint8_t[16]));
  uint8_t a[] = {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15};
  uint8_t * b = a;
  printf("sizeof byte[]   %d\n", sizeof(a));
  printf("sizeof byte[]   %d\n", sizeof(b));
  return 0;
}

