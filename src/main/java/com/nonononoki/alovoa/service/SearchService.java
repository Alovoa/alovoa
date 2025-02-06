package com.nonononoki.alovoa.service;

import com.nonononoki.alovoa.Tools;
import com.nonononoki.alovoa.component.TextEncryptorConverter;
import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.entity.user.Gender;
import com.nonononoki.alovoa.entity.user.UserIntention;
import com.nonononoki.alovoa.model.AlovoaException;
import com.nonononoki.alovoa.model.SearchDto;
import com.nonononoki.alovoa.model.SearchDto.SearchStage;
import com.nonononoki.alovoa.model.UserDto;
import com.nonononoki.alovoa.model.UserSearchRequest;
import com.nonononoki.alovoa.repo.GenderRepository;
import com.nonononoki.alovoa.repo.UserRepository;
import lombok.Builder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import uk.recurse.geocoding.reverse.Country;
import uk.recurse.geocoding.reverse.ReverseGeocoder;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {

    public static final int SORT_DISTANCE = 1;
    public static final int SORT_ACTIVE_DATE = 2;
    public static final int SORT_INTEREST = 3;
    public static final int SORT_DEFAULT = 4;
    public static final int SORT_DONATION_TOTAL = 5;
    public static final int SORT_NEWEST_USER = 6;
    private static final double LATITUDE = 111.1;
    private static final double LONGITUDE = 111.320;
    private static final int DEFAULT_DISTANCE = 50;
    private static final int SEARCH_MAX = 200;
    private static ReverseGeocoder geocoder = null;

    private static final Set<Long> ALL_INTENTIONS = Set.of(UserIntention.MEET, UserIntention.DATE, UserIntention.SEX);
    private static final Set<Long> ALL_GENDER_IDS = Set.of(Gender.MALE, Gender.FEMALE, Gender.OTHER);

    @Autowired
    private TextEncryptorConverter textEncryptor;
    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepo;
    @Autowired
    private PublicService publicService;
    @Autowired
    private UserService userService;
    @Autowired
    private GenderRepository genderRepo;
    @Value("${app.search.max}")
    private int maxResults;
    @Value("${app.search.max.distance}")
    private int maxDistance;
    @Value("${app.age.min}")
    private int ageMin;
    @Value("${app.age.max}")
    private int ageMax;
    @Value("${app.search.estimate.max}")
    private int searchEstimateMax;
    @Value("${app.search.ignore-intention}")
    private boolean ignoreIntention;

    public ReverseGeocoder getGeocoder() {
        if (geocoder == null) {
            geocoder = new ReverseGeocoder();
        }
        return geocoder;
    }

    @Deprecated
    public SearchDto searchDefault() throws AlovoaException, InvalidKeyException, IllegalBlockSizeException,
            BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException,
            UnsupportedEncodingException {
        User user = authService.getCurrentUser(true);
        if (user.isAdmin()) {
            return SearchDto.builder().users(searchResultsToUserDto(userRepo.adminSearch(), 0, user)).build();
        }
        if (user.getLocationLatitude() != null) {
            return searchComplete(SearchParams.builder().latitude(user.getLocationLatitude())
                    .longitude(user.getLocationLongitude()).build());
        } else {
            return null;
        }
    }

    @Deprecated
    public SearchDto search(Double latitude, Double longitude, int distance, int sortId) throws AlovoaException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException {
        return searchComplete(SearchParams.builder().latitude(latitude)
                .longitude(longitude).distance(distance).sort(sortId).build());
    }

    public SearchDto searchComplete(SearchParams params) throws AlovoaException,
            InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException {

        User user = authService.getCurrentUser(true);
        if (user.isAdmin()) {
            return SearchDto.builder().users(searchResultsToUserDto(userRepo.adminSearch(), 0, user)).build();
        }

        Double latitude = params.getLatitude();
        Double longitude = params.getLongitude();

        int distance = params.getDistance();
        int sortId = params.getSort();
        boolean showOutsideParameters = params.isShowOutsideParameters();
        Set<Long> intentions = params.getIntentions().isEmpty() ? ALL_INTENTIONS : params.getIntentions();
        Set<String> interests = params.getInterests();
        Set<Integer> miscInfoIds = params.getMiscInfos();

        Set<Long> preferredGenderIds;
        Set<Long> userPreferredGenderIds =  user.getPreferedGenders().stream().map(Gender::getId).collect(Collectors.toSet());
        boolean updatedPreferredGenderIds = !params.getPreferredGenderIds().isEmpty() && !Objects.equals(userPreferredGenderIds, params.getPreferredGenderIds());
        if(updatedPreferredGenderIds)  {
            user.setPreferedGenders(new HashSet<>(genderRepo.findAllById(params.getPreferredGenderIds())));
            preferredGenderIds = params.getPreferredGenderIds();
        } else {
            preferredGenderIds = userPreferredGenderIds;
        }

        if (!latitude.equals(user.getLocationLatitude()) || !longitude.equals(user.getLocationLongitude()) || user.getCountry() == null) {
            Optional<String> country = getCountryIsoByLocation(latitude, longitude);
            if (country.isPresent()) {
                userService.updateCountry(country.get());
            }
        }

        Sort sort = switch (sortId) {
            case SORT_DEFAULT -> Sort.by(Sort.Direction.DESC, "dates.latestDonationDate", "dates.creationDate");
            case SORT_ACTIVE_DATE -> Sort.by(Sort.Direction.DESC, "dates.activeDate");
            case SORT_DONATION_TOTAL -> Sort.by(Sort.Direction.DESC, "totalDonations");
            case SORT_NEWEST_USER -> Sort.by(Sort.Direction.DESC, "dates.creationDate");
            default -> Sort.unsorted();
        };

        int ageLegal = Tools.AGE_LEGAL;

        if (distance > maxDistance) {
            throw new AlovoaException("max_distance_exceeded");
        }

        user.getDates().setActiveDate(new Date());

        userService.updateUserLocation(latitude, longitude);

        int age = Tools.calcUserAge(user);
        boolean isLegalAge = age >= ageLegal;
        int minAge = params.getPreferredMinAge() == null ? user.getPreferedMinAge() : params.getPreferredMinAge();
        int maxAge = params.getPreferredMaxAge() == null ? user.getPreferedMaxAge() : params.getPreferredMaxAge();

        user.setPreferedMinAge(minAge);
        user.setPreferedMaxAge(maxAge);

        if (isLegalAge && minAge < ageLegal) {
            minAge = ageLegal;
        }
        if (!isLegalAge && maxAge >= ageLegal) {
            maxAge = ageLegal - 1;
        }

        Date minDate = Tools.ageToDate(maxAge);
        Date maxDate = Tools.ageToDate(minAge);

        double deltaLongFactor = LONGITUDE * Math.cos(latitude / 180.0 * Math.PI);
        double deltaLat = distance / LATITUDE;
        double deltaLong = distance / deltaLongFactor;
        double minLat = latitude - deltaLat;
        double maxLat = latitude + deltaLat;
        double minLong = longitude - deltaLong;
        double maxLong = longitude + deltaLong;

        userRepo.saveAndFlush(user);

        UserSearchRequest request = UserSearchRequest.builder().age(age).minLat(minLat).minLong(minLong).maxLat(maxLat)
                .maxLong(maxLong).maxDateDob(maxDate).minDateDob(minDate).intentionIds(intentions).preferedGender(user.getGender())
                .likeIds(user.getLikes().stream().map(o -> o.getUserTo() != null ? o.getUserTo().getId() : 0).collect(Collectors.toSet()))
                .blockIds(user.getBlockedUsers().stream().map(o -> o.getUserTo() != null ? o.getUserTo().getId() : 0).collect(Collectors.toSet()))
                .hideIds(user.getHiddenUsers().stream().map(o -> o.getUserTo() != null ? o.getUserTo().getId() : 0).collect(Collectors.toSet()))
                .genderIds(preferredGenderIds)
                .blockedByIds(user.getBlockedByUsers().stream().map(o -> o.getUserFrom() != null ? o.getUserFrom().getId() : 0).collect(Collectors.toSet()))
                .miscInfos(miscInfoIds)
                .interests(interests)
                .build();

        request.getBlockIds().add(user.getId());

        List<User> users;
        if(request.getMiscInfos().isEmpty() && request.getInterests().isEmpty()) {
            users = userRepo.usersSearchNoExtras(request, PageRequest.of(0, SEARCH_MAX, sort));
        } else if(!request.getMiscInfos().isEmpty() && request.getInterests().isEmpty()) {
            users = userRepo.usersSearchMisc(request, PageRequest.of(0, SEARCH_MAX, sort));
        } else if(!request.getInterests().isEmpty() && request.getMiscInfos().isEmpty()) {
            users = userRepo.usersSearchInterest(request, PageRequest.of(0, SEARCH_MAX, sort));
        } else {
            users = userRepo.usersSearchInterestMisc(request, PageRequest.of(0, SEARCH_MAX, sort));
        }

        if (!users.isEmpty()) {
            return SearchDto.builder().users(searchResultsToUserDto(users, sortId, user))
                    .stage(SearchStage.NORMAL).build();
        }

        // NO COMPATIBLE USERS FOUND
        if(showOutsideParameters) {

            //SEARCH COMPATIBLE USERS
            request.setIntentionIds(ALL_INTENTIONS);
            request.setMiscInfos(Set.of());
            users = userRepo.usersBaseSearch(request, PageRequest.of(0, SEARCH_MAX, sort));
            if (!users.isEmpty()) {
                return SearchDto.builder().users(searchResultsToUserDto(users, sortId, user))
                        .global(false).build();
            }

            // NO COMPATIBLE USERS FOUND NEARBY, SEARCH AROUND THE WORLD!
            users = userRepo.usersSearchAllIgnoreLocation(request, PageRequest.of(0, SEARCH_MAX, sort));
                return SearchDto.builder().users(searchResultsToUserDto(users, sortId, user))
                        .global(true).incompatible(true).stage(SearchStage.WORLD).build();
        } else {
            return SearchDto.builder().users(List.of()).build();
        }
    }

    private List<UserDto> searchResultsToUserDto(final List<User> userList, int sort, User user)
            throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, AlovoaException {
        List<UserDto> userDtos = new ArrayList<>();
        for (User u : userList) {
            UserDto dto = UserDto.userToUserDto(UserDto.DtoBuilder.builder().ignoreIntention(ignoreIntention)
                    .currentUser(user).user(u).userService(userService).build());
            userDtos.add(dto);
        }

        if (sort == SORT_DISTANCE) {
            userDtos = userDtos.stream().sorted(Comparator.comparing(UserDto::getDistanceToUser))
                    .collect(Collectors.toList());
        } else if (sort == SORT_INTEREST) {
            Comparator<UserDto> comparatorCommonInterest = Comparator.comparing(f -> f.getCommonInterests().size());
            userDtos = userDtos.stream().filter(f -> !f.getCommonInterests().isEmpty())
                    .sorted(comparatorCommonInterest.reversed()
                            .thenComparing(Comparator.comparing(UserDto::getDistanceToUser).reversed()))
                    .collect(Collectors.toList());
        }

        return userDtos;
    }

    public Optional<String> getCountryIsoByLocation(double lat, double lon) {
        Optional<Country> country = getGeocoder().getCountry(lat, lon);
        return country.map(Country::iso);
    }

    @Getter
    @Builder
    public static class SearchParams {
        @Builder.Default
        private Set<Long> preferredGenderIds = new HashSet<>();
        @Builder.Default
        private Integer preferredMinAge = null;
        @Builder.Default
        private Integer preferredMaxAge = null;
        @Builder.Default
        private int distance = DEFAULT_DISTANCE;
        @Builder.Default
        private boolean showOutsideParameters = true;
        @Builder.Default
        private int sort = SORT_DEFAULT;
        private double latitude;
        private double longitude;
        @Builder.Default
        private Set<Integer> miscInfos = new HashSet<>();
        @Builder.Default
        private Set<Long> intentions = new HashSet<>();
        @Builder.Default
        private Set<String> interests = new HashSet<>();
    }

}
